package de.reneruck.tcd.ipp.andclient.actions;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.transition.Transition;
import de.reneruck.tcd.ipp.datamodel.transition.TransitionExchangeBean;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;

public class ReceiveData implements Action {

	private ObjectOutputStream out;
	private SharedPreferences transitionQueue;
	private TransitionExchangeBean bean;
	
	public ReceiveData(TransitionExchangeBean transitionExchangeBean, SharedPreferences transitionsQueue) {
		this.bean = transitionExchangeBean;
		this.transitionQueue = transitionsQueue;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		if(this.out == null) {
			this.out = this.bean.getOut();
		}
		Object content = event.getParameter(Statics.CONTENT_TRANSITION);
		if(content != null && content instanceof Transition) {
			Transition transition = (Transition)content;
			String transitonId = "T" + transition.getTransitionId();
			boolean contains = this.transitionQueue.contains(transitonId);
			
			if(contains) {
				Editor edit = this.transitionQueue.edit();
				edit.remove(transitonId);
				edit.putString(transitonId, transition.toString());
				edit.apply();
			} else {
				/*
				 * Store all Transitions that does not belong to this device into
				 * a seperate queue to share them with other devices when they come online 
				 */
			}
			sendAck(content);
		} else {
			System.err.println("Invalid event content");
		}
	}

	private void sendAck(Object content) throws IOException {
		Map<String, Object> datagramPayload = new HashMap<String, Object>();
		datagramPayload.put(Statics.TRAMSITION_ID, ((Transition)content).getTransitionId());
		this.out.writeObject(new Datagram(Statics.ACK, datagramPayload));
		this.out.flush();
	}

}
