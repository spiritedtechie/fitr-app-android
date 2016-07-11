package fitr.mobile.presenters;

import fitr.mobile.views.View;

public interface Presenter<T extends View> {

    void attachView(T view);

    void detachView();
}
