package fitr.mobile;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Session;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import fitr.mobile.google.FitnessRecordingHelper;
import fitr.mobile.google.FitnessSessionHelper;
import rx.functions.Func1;

import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ActivityRecordingFragment extends Fragment {

    private static final String TAG = "ActivityRecording";

    private static final String DATE_FORMAT_PATTERN_DEFAULT = "yyyy-MM-dd'T'HH:mm:ss";

    private Spinner dropDownListActivityType;

    @Inject
    FitnessRecordingHelper frh;
    @Inject
    FitnessSessionHelper fsh;
    private Session session;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((Injector) getActivity()).inject(this);

        View view = inflater.inflate(R.layout.fragment_fitness_activity, container, false);

        dropDownListActivityType = (Spinner) view.findViewById(R.id.spinner_activity_type);
        Button btnStart = (Button) view.findViewById(R.id.btn_start);
        Button btnStop = (Button) view.findViewById(R.id.btn_stop);

        initialiseActivitySpinner();

        // Button listeners
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subscribe();
                startSession();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSession();
                unsubscribe();
            }
        });

        return view;
    }

    private void initialiseActivitySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, buildActivitiesList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropDownListActivityType.setAdapter(adapter);
    }

    private void subscribe() {
        frh.subscribeIfNotExistingSubscription(TYPE_DISTANCE_DELTA)
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    private void unsubscribe() {
        frh.unsubscribe(TYPE_DISTANCE_DELTA)
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    private void startSession() {
        if (session == null) {
            session = buildSession();
            fsh.startSession(session)
                    .onErrorReturn(loggingErrorHandler())
                    .subscribe();
        }
    }

    private Session buildSession() {
        String selectedActivity = dropDownListActivityType.getSelectedItem().toString();
        Date currTime = new Date();
        String sessionName = selectedActivity + "-" + formatTime(currTime.getTime());

        return new Session.Builder()
                .setName(sessionName)
                .setIdentifier(UUID.randomUUID().toString())
                .setDescription("Session of " + selectedActivity)
                .setStartTime(currTime.getTime(), MILLISECONDS)
                .setActivity(selectedActivity)
                .build();
    }

    private void stopSession() {
        if (session == null || !session.isOngoing()) {
            return;
        }

        fsh.stopSession(session.getIdentifier())
                .firstOrDefault(null)
                .map(new Func1<Session, Void>() {
                    @Override
                    public Void call(Session session) {
                        notifySessionStopped(session);
                        return null;
                    }
                })
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    private void notifySessionStopped(Session session) {
        if (session != null) {
            String toastMsg = "Session stopped." +
                    "\nIt started at " + formatTime(session.getStartTime(MILLISECONDS)) +
                    "\nIt ended at " + formatTime(session.getEndTime(MILLISECONDS));
            Toast.makeText(getContext(), toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    private List<String> buildActivitiesList() {
        List<String> activities = new ArrayList<>();
        activities.add(FitnessActivities.RUNNING);
        activities.add(FitnessActivities.WALKING);
        activities.add(FitnessActivities.BIKING);
        return activities;
    }

    private String formatTime(long timeMillis) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN_DEFAULT);
        return df.format(new Date(timeMillis));
    }

    private Func1<Throwable, Void> loggingErrorHandler() {
        return new Func1<Throwable, Void>() {
            @Override
            public Void call(Throwable t) {
                Log.e(TAG, t.getMessage(), t);
                return null;
            }
        };
    }

    interface Injector {
        void inject(ActivityRecordingFragment frag);
    }

}
