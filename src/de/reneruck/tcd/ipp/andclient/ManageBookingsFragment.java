package de.reneruck.tcd.ipp.andclient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import de.reneruck.tcd.ipp.datamodel.Booking;
import de.reneruck.tcd.ipp.datamodel.transition.CancelBookingTransition;
import de.reneruck.tcd.ipp.datamodel.transition.NewBookingTransition;
import de.reneruck.tcd.ipp.datamodel.transition.Transition;
import de.reneruck.tcd.ipp.datamodel.transition.TransitionState;

public class ManageBookingsFragment extends Fragment {

	private static final Date THREE_DAYS = new Date(System.currentTimeMillis() + (3 * 86400000));
	private static final String TAG = null;
	private Gson gson;
	private Object selectedItemTag;

	public ManageBookingsFragment() {
		this.gson = new Gson();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	TableLayout layout = (TableLayout) inflater.inflate(R.layout.fragment_manage_bookings, null);
    	SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Bookings", Context.MODE_PRIVATE);

    	Map<String, ?> all = sharedPreferences.getAll();
    	Set<String> keySet = all.keySet();

    	for (String key : keySet) {
			TableRow row = (TableRow) inflater.inflate(R.layout.manage_bookings_row, null);
			TextView type = (TextView) row.findViewById(R.id.transition_type);
			TextView id = (TextView) row.findViewById(R.id.transition_id);
			TextView status = (TextView) row.findViewById(R.id.transition_status);
			
			Object deserialized = deserialize(all.get(key));
			
			if(deserialized instanceof Transition) {
				
				Transition transition = (Transition) deserialized;
				if(TransitionState.PROCESSED.equals(transition.getTransitionState())) {
					if(transition.getBooking().isAccepted()) {
						status.setTextColor(Color.GREEN);
					} else {
						status.setTextColor(Color.RED);
					}
				}
				
				setType(transition, type);
				String idString = "T" + transition.getTransitionId();
				id.setText(idString);
				status.setText(transition.getTransitionState().toString());
				row.setTag(idString);
				layout.addView(row);
				registerForContextMenu(row);
			}
		}
        return layout;
    }
	
	private void setType(Transition transition, TextView type) {
		if(transition instanceof NewBookingTransition) {
			type.setText("NBT");
		} else if(transition instanceof CancelBookingTransition) {
			type.setText("CBT");
		} else {
			type.setText("?");
		}
	}

	private Object deserialize(Object readObject) {
		if (readObject instanceof String) {
			String input = (String) readObject;
			String[] split = input.split("=");
			if (split.length > 1) {
				try {
					Class<?> transitionClass = Class.forName(split[0]);
					Object fromJson = this.gson.fromJson(split[1].trim(), transitionClass);
					System.out.println("Successfully deserialized ");
					return fromJson;
				} catch (ClassNotFoundException e) {
					e.getMessage();
					System.err.println("Cannot deserialize, discarding package");
				}
			} else {
				System.err.println("No valid class identifier found, discarding package");
			}
		}
		return readObject;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		this.selectedItemTag = v.getTag();
		MenuInflater inflater = getMenuInflater();
		menu.setHeaderTitle("Booking Options");
	    inflater.inflate(R.menu.manage_bookings_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Bookings", Context.MODE_PRIVATE);
		Transition transition = getTransitionForId((String)selectedItemTag, sharedPreferences);

		switch (item.getItemId()) {
			case R.id.details:
				Log.d(TAG, "Details selected");
				FragmentManager fragmentManager = getFragmentManager();
				FragmentTransaction ft = fragmentManager.beginTransaction();
				String details = "";
				
				if(transition != null) {
					details = getBookingDetails(transition);
				} else {
					details = "Booking details are not available";
				}
				ft.add(new InfoDialog("Booking Details", details ), "infoDialog");
				ft.commit();
				break;
			case R.id.cancel:
				Log.d(TAG, "Cancel booking selected");
				if(selectedItemTag != null) {
					
					if(transition != null) {
						handleTransition(sharedPreferences, transition);
					} else {
						Toast.makeText(getActivity(), "The selected booking cannot be found or is no Booking request that can ", Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(getActivity(), "Error getting choosen item", Toast.LENGTH_LONG).show();
				}
				break;
	
			default:
				break;
			}
		return true;
	}

	private String getBookingDetails(Transition transition) {
		Booking booking = transition.getBooking();
		SimpleDateFormat formater = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		return "Details: \n" +
				"ID: " + booking.getId() + "\n" +
				"Name: " + booking.getRequester() + "\n" +
				"From: " + booking.getFrom() + "\n" +
				"Date: " + formater.format(booking.getTravelDate());
	}

	private void handleTransition(SharedPreferences sharedPreferences, Transition transition) {
		if(TransitionState.PENDING.equals(transition.getTransitionState())){
			if(transition.getBooking().getTravelDate().after(THREE_DAYS)) { // no booking cancelation if flight < three days 
				createAndSaveCancelBooking(transition.getBooking(), sharedPreferences);
			} else {
				FragmentManager fragmentManager = getFragmentManager();
				FragmentTransaction ft = fragmentManager.beginTransaction();
				ft.add(new InfoDialog("Cancelation not possible", "No cancelation of bookings nearer than 3 days possible"), "infoDialog");
				ft.commit();
			}
		} else {
			Toast.makeText(getActivity(), "The selected booking cannot be canceled", Toast.LENGTH_LONG).show();
		}
	}

	private void createAndSaveCancelBooking(Booking booking, SharedPreferences sharedPreferences) {
		Editor edit = sharedPreferences.edit();
		CancelBookingTransition cancelTransition = new CancelBookingTransition(booking);
		edit.putString("T" + cancelTransition.getTransitionId(), cancelTransition.toString());
		edit.apply();
	}
	
	private Transition getTransitionForId(String selectedItemTag, SharedPreferences sharedPreferences) {
		String serializedTransition = sharedPreferences.getString(selectedItemTag, "");
		Object transition = deserialize(serializedTransition);
		if(transition instanceof NewBookingTransition) {
			return (Transition)transition;
		}
		return null;
	}

	private MenuInflater getMenuInflater() {
		return new MenuInflater(getActivity());
	}
	
	class InfoDialog extends DialogFragment {
		
		private String message;
		private String title;

		public InfoDialog(String title, String message) {
			this.title = title;
			this.message = message;
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(this.title);
			builder.setMessage(this.message);
			builder.setCancelable(true);
			
			return builder.create();
		}
	}
}
