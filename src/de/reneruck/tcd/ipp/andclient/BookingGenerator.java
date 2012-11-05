package de.reneruck.tcd.ipp.andclient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.reneruck.tcd.ipp.datamodel.Airport;
import de.reneruck.tcd.ipp.datamodel.Booking;
import de.reneruck.tcd.ipp.datamodel.transition.NewBookingTransition;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;

public class BookingGenerator extends AsyncTask<String, Void, Void> {

	private Context context;
	
	public BookingGenerator(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(String... params) {
		String param = params[0];
		
		// clean time for parsing
		if(param.endsWith("h")) {
			param = param.substring(0, param.length()-1);
		}
		
		String[] split = param.split(";");
		
		param = split[0];
		Airport departureAiport = getdepartureAirport(split[1]);
		
		SimpleDateFormat formater = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		Calendar calendar = Calendar.getInstance();

		try {
			Date parsedDate = formater.parse(param);
			calendar.setTime(parsedDate);
			
			Booking booking = new Booking("John Doe", calendar.getTime(), departureAiport);
			
			NewBookingTransition bookingTransition = new NewBookingTransition(booking);
			
			SharedPreferences pref = this.context.getSharedPreferences("Bookings", Context.MODE_PRIVATE);
			Editor edit = pref.edit();
			edit.putString("T" + bookingTransition.getTransitionId(), bookingTransition.toString());
			edit.apply();
			Log.d("BookingGenerator", "Stored Booking " + booking.toString());
		} catch (ParseException e) {
			e.printStackTrace();
		}

			
		return null;
	}



	private Airport getdepartureAirport(String split) {
		Airport airport = Airport.valueOf(split);
		switch (airport) {
		case camp:
			return Airport.city;
		case city:
			return Airport.camp;
		default:
			break;
		}
		return Airport.camp;
	}

}
