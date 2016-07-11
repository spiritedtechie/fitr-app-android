package fitr.mobile.presenters;

import android.util.Log;

import fitr.mobile.views.View;
import rx.Subscription;

public class BasePresenter<T extends View> implements Presenter<T> {

    private T view;

    @Override
    public void attachView(T view) {
        this.view = view;
    }

    @Override
    public void detachView() {
        view = null;
    }

    public boolean isViewAttached() {
        return view != null;
    }

    public T getView() {
        return view;
    }

    protected void unsubscribe(Subscription... subscriptions) {
        Log.i("BasePresenter", "Unsubscribing Rx observable subscriptions");
        for (Subscription s : subscriptions) {
            if (s != null) {
                s.unsubscribe();
            }
        }
    }

    public void checkViewAttached() {
        if (!isViewAttached()) throw new ViewNotAttachedException();
    }

    public static class ViewNotAttachedException extends RuntimeException {
        public ViewNotAttachedException() {
            super("Please call Presenter.attachView(View) before" +
                    " requesting data to the Presenter");
        }
    }
}