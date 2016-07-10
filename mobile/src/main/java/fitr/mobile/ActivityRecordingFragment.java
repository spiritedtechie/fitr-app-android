package fitr.mobile;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import javax.inject.Inject;

import fitr.mobile.presenters.ActivityRecordingPresenter;
import fitr.mobile.views.ActivityRecordingView;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ActivityRecordingFragment extends Fragment implements
        ActivityRecordingView {

    private static final String TAG = "ActivityRecording";

    private static final String DATE_FORMAT_PATTERN_DEFAULT = "yyyy-MM-dd'T'HH:mm:ss";

    @Inject
    ActivityRecordingPresenter presenter;

    private Button btnStart;
    private Button btnStop;
    private Spinner dropDownListActivityType;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((Injector) getActivity()).inject(this);

        View view = inflater.inflate(R.layout.fragment_fitness_activity, container, false);

        dropDownListActivityType = (Spinner) view.findViewById(R.id.spinner_activity_type);
        btnStart = (Button) view.findViewById(R.id.btn_start);
        btnStop = (Button) view.findViewById(R.id.btn_stop);

        initialiseActivitySpinner();

        // Button listeners
        btnStop.setEnabled(false);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.subscribe();
                String selectedActivity = dropDownListActivityType.getSelectedItem().toString();
                presenter.startSession(ActivityRecordingFragment.this, selectedActivity);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.stopSession(ActivityRecordingFragment.this);
                presenter.unsubscribe();
            }
        });

        return view;
    }

    private void initialiseActivitySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, buildActivitiesList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropDownListActivityType.setAdapter(adapter);
    }

    private List<String> buildActivitiesList() {
        List<String> activities = new ArrayList<>();
        activities.add(FitnessActivities.RUNNING);
        activities.add(FitnessActivities.WALKING);
        activities.add(FitnessActivities.BIKING);
        return activities;
    }

    @Override
    public void sessionStarting(Session session) {
        btnStart.setEnabled(false);
    }

    @Override
    public void sessionStarted(Session session) {
        btnStop.setEnabled(true);
        Toast.makeText(getContext(), "Session started!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void sessionStopping(Session session) {
        btnStop.setEnabled(false);
    }

    @Override
    public void sessionStopped(Session session) {
        btnStart.setEnabled(true);

        String toastMsg = "Session stopped." +
                "\nIt started at " + formatTime(session.getStartTime(MILLISECONDS)) +
                "\nIt ended at " + formatTime(session.getEndTime(MILLISECONDS));
        Toast.makeText(getContext(), toastMsg, Toast.LENGTH_LONG).show();
    }

    private String formatTime(long timeMillis) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN_DEFAULT);
        return df.format(new Date(timeMillis));
    }


    interface Injector {
        void inject(ActivityRecordingFragment frag);
    }

}
