package fitr.mobile.presenters;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fitr.mobile.views.View;
import rx.Subscription;

public class BasePresenter<T extends View> implements Presenter<T> {

    private static final String TAG = BasePresenter.class.getSimpleName();

    private List<Subscription> managedSubscriptions = new ArrayList<>();

    private T view;

    interface DoOnView<T> {
        void execute(T view);
    }

    @Override
    public void attachView(T view) {
        this.view = view;
        this.onViewAttached();
    }

    protected void onViewAttached() {
        // by default do nothing
    }

    @Override
    public void detachView() {
        view = null;
        removeSubscriptions();
    }

    public void onView(DoOnView<T> action) {
        if (view != null) {
            action.execute(view);
        }
    }

    protected void manage(Subscription subscription) {
        if (managedSubscriptions == null) {
            managedSubscriptions = new ArrayList<>();
        }

        managedSubscriptions.add(subscription);
    }

    private void removeSubscriptions() {
        Log.i(TAG, "Unsubscribing Rx observable subscriptions");
        if (managedSubscriptions != null) {
            Log.i(TAG, "Removing " + managedSubscriptions.size() + " observable subscriptions");
            for (Subscription s : managedSubscriptions) {
                if (s != null) {
                    s.unsubscribe();
                }
            }
        }
    }

}