package fitr.mobile;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fitr.mobile.presenters.RegisterPresenter;
import fitr.mobile.views.RegisterView;

public class RegisterActivity extends AppCompatActivity implements RegisterView {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.entry_username)
    TextView userNameEntryTextView;

    @BindView(R.id.entry_password)
    TextView passwordEntryTextView;

    @Inject
    RegisterPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inject
        ButterKnife.bind(this);
        ((FitnessApplication) getApplication()).getFitnessComponent().inject(this);

        // Set a Toolbar to replace the ActionBar.
        setSupportActionBar(toolbar);

        presenter.attachView(this);
    }

    @Override
    protected void onDestroy() {
        presenter.detachView();

        super.onDestroy();
    }

    @OnClick(R.id.btn_register)
    public void registerClick() {
        presenter.register(
                userNameEntryTextView.getText().toString(),
                passwordEntryTextView.getText().toString());

    }

    @Override
    public void registrationSuccessful() {
        Toast.makeText(getBaseContext(), "You have signed up!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void registrationFailure() {
        Toast.makeText(
                getBaseContext(),
                "Signup failed. You may have registered already or there is a technical problem.",
                Toast.LENGTH_LONG).show();
    }
}
