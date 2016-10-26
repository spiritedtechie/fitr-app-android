package fitr.mobile;

import android.os.Bundle;
import android.support.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fitr.mobile.presenters.ActivityRecordingPresenter;
import fitr.mobile.views.ActivityRecordingView;

public class ActivityRecordingFragment extends Fragment implements
        ActivityRecordingView {

    private static final String TAG = ActivityRecordingFragment.class.getSimpleName();

    @Inject
    ActivityRecordingPresenter presenter;

    @BindView(R.id.btn_start)
    Button btnStart;
    @BindView(R.id.btn_stop)
    Button btnStop;
    @BindView(R.id.spinner_activity_type)
    Spinner dropDownListActivityType;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_fitness_activity, container, false);
        ButterKnife.bind(this, view);
        ((Injector) getActivity()).inject(this);

        initialiseActivitySpinner();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.attachView(this);

        // Button listeners
        btnStart.setOnClickListener(v -> {
            presenter.subscribe();
            String selectedActivity = dropDownListActivityType.getSelectedItem().toString();
            presenter.startSession(selectedActivity);
        });

        btnStop.setOnClickListener(v -> {
            presenter.stopSession();
            presenter.unsubscribe();
        });
    }

    @Override
    public void onDestroyView() {
        presenter.detachView();
        super.onDestroyView();
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
    public void allowStartSession(boolean allow) {
        btnStart.setEnabled(allow);
    }

    @Override
    public void allowStopSession(boolean allow) {
        btnStop.setEnabled(allow);
    }

    @Override
    public void sessionStarted(Session session) {
        Toast.makeText(getContext(), "Session started!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void sessionStopped(Session session) {
        String toastMsg = "Session stopped.";
        Toast.makeText(getContext(), toastMsg, Toast.LENGTH_LONG).show();
    }

    interface Injector {
        void inject(ActivityRecordingFragment frag);
    }

}
