package fitr.mobile;

import android.os.Bundle;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class AcceptedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accepted);

        String submittedName = getIntent().getStringExtra("NAME");

        TextView tv_name_submitted = (TextView) findViewById(R.id.tv_name_submitted);

        tv_name_submitted.setText(submittedName);
    }
}