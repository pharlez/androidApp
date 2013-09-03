package gr.unfold.android.tsibato;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class TutorialActivity extends Activity implements OnClickListener {
	
	private static final String TAG = TutorialActivity.class.getName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.tutorial);
        
        findViewById(R.id.tutorial_gotit).setOnClickListener(this);

	}
	
	@Override
	public void onClick(View v) {
		Intent returnIntent = new Intent();
		switch (v.getId()) {
			case R.id.tutorial_gotit:
				setResult(RESULT_OK, returnIntent);     
				finish();
				break;
		}
	}

}
