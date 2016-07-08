package fitr.mobile;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WorkoutTabsFragment extends Fragment {

    private static final String TAG = "WorkoutTabs";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_container_tab_layout, container, false);

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.sliding_tabs);

        viewPager.setAdapter(new WorkoutTabsAdapter(getChildFragmentManager(), getContext()));
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }

}
