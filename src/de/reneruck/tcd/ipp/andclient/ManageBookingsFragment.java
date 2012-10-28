package de.reneruck.tcd.ipp.andclient;

import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import de.reneruck.tcd.ipp.datamodel.transition.Transition;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ManageBookingsFragment extends Fragment {

	private Gson gson;

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
			TextView id = (TextView) row.findViewById(R.id.transition_id);
			TextView status = (TextView) row.findViewById(R.id.transition_status);
			
			Object transition = deserialize(all.get(key));
			
			if(transition instanceof Transition) {
				id.setText("T" + ((Transition) transition).getTransitionId());
				status.setText(((Transition) transition).getTransitionState().toString());
				layout.addView(row);
			}
		}
        return layout;
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
}
