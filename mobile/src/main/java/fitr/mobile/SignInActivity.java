package fitr.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fitr.mobile.presenters.SignInPresenter;
import fitr.mobile.views.SignInView;

public class SignInActivity extends AppCompatActivity implements SignInView {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.entry_username)
    TextView userNameEntryTextView;

    @BindView(R.id.entry_password)
    TextView passwordEntryTextView;

    @Inject
    SignInPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        // Inject
        ButterKnife.bind(this);
        ((FitnessApplication) getApplication()).getFitnessComponent().inject(this);

        // Set a Toolbar to replace the ActionBar.
        setSupportActionBar(toolbar);
    }

    @OnClick(R.id.btn_signin)
    public void login() {
        presenter.signIn(
                userNameEntryTextView.getText().toString(),
                passwordEntryTextView.getText().toString());
    }

    @OnClick(R.id.btn_signup)
    public void signupClick() {
        Intent i = new Intent(getBaseContext(), RegisterActivity.class);
        startActivity(i);
    }

}
