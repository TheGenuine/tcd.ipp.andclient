package de.reneruck.tcd.ipp.andclient.actions;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.content.SharedPreferences;
import de.reneruck.tcd.ipp.datamodel.Callback;
import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.Transition;
import de.reneruck.tcd.ipp.datamodel.TransitionExchangeBean;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;

public class SendData implements Action, Callback {

	private ObjectOutputStream out;
	private DataSender sender;
	private Map<Long, Transition> dataset = new HashMap<Long, Transition>();
	private SharedPreferences transitionsStore;
	private TransitionExchangeBean bean;

	public SendData(TransitionExchangeBean transitionExchangeBean, SharedPreferences transitionsStore ) {
		this.bean = transitionExchangeBean;
		this.transitionsStore = transitionsStore;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		if(this.out == null) {
			this.out = this.bean.getOut();
		}
		
		send(Statics.RX_HELI_ACK);
		
		if(this.sender == null) {
			initializeDataSender();
		}
		
		if(Statics.ACK.equals(event.getIdentifier())) {
			Object parameter = event.getParameter(Statics.TRAMSITION_ID);
			if(parameter != null && parameter instanceof Long) {
				this.dataset.remove(parameter);
			}
		}
	}

	private void initializeDataSender() {
		createDataset();
		this.sender = new DataSender(this.out, this.dataset, this);
		this.sender.start();
	}

	private void createDataset() {
		Map<String, ?> all = this.transitionsStore.getAll();
		Collection<Transition> allTransitions = (Collection<Transition>) all.values();
		for (Transition transition : allTransitions) {
			this.dataset.put(transition.getTransitionId(), transition);
		}
	}

	@Override
	public void handleCallback() {
		this.sender = null;
		try {
			this.bean.getFsm().handleEvent(new TransitionEvent(Statics.FINISH_RX_HELI));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void send(String message) throws IOException {
		this.out.writeObject(new Datagram(message));
		this.out.flush();
	}

}
