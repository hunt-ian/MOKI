package com.example.ian.mobile_oki.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.ian.mobile_oki.R;
import com.example.ian.mobile_oki.contracts.MainMenuContract;
import com.example.ian.mobile_oki.logic.MainMenuPresenter;

/**
 * Shortening the name to MOKI, since I had to make another Git repo.
 *
 * Despite all my efforts, there seems to be no way to allow orientation changes AND remain bug-free.
 * Orientation changes during the app's startup or between activities causes the character/kd to not be
 * properly set or causes them to reset to null. Even debugging line by line, there is no visible reason
 * for why they are being reset. I'm forced to just lock orientation change. I simply can't figure it out.
 *
 * <p>
 * TODO: Lock orientation to portrait mode to [avoid/contain/quarantine] the orientation change bugs. (This won't solve keyboard config change...)
 * <p>
 * TODO: Build test classes which can handle/test the Activities.
 * <p>
 * TODO: Remove click listener implementation unless it turns out it's needed for the coming buttons
 * <p>
 **/
public class MainActivity extends AppCompatActivity implements MainMenuContract.View {

//    public static final String EXTRA_MESSAGE = "com.example.ian.MESSAGE";
    public static final int CHAR_SEL_REQUEST_CODE = 6969;
    public static final int KD_MOVE_SEL_REQUEST_CODE = 8008;
    public static final String CHARACTER_EXTRA = "selected-character";
    public static final String KD_MOVE_EXTRA = "selected-kd-move";

    private static final String TAG = MainActivity.class.getSimpleName();

//    private Toast mToast;

    /**
     * The currently selected character.
     * <p>Holds the 3-letter character code corresponding to a database table name.
     * <p><i>(e.g. Alex = ALX)</i>
     */
    private String mSelectedCharacter;

    /**
     * The currently selected Knockdown Move.
     * <p>Holds the entire move name as listed in the database.
     */
    private String mSelectedKDMove;

    private MainMenuContract.Presenter mMainMenuPresenter;

    private TableLayout mTimeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mToast = new Toast(getApplicationContext());

        // get or create presenter instance, which will in turn set this view's presenter
        mMainMenuPresenter = MainMenuPresenter.getInstance(this, getApplicationContext());
        setPresenter(mMainMenuPresenter);

        // get and hide timeline
        mTimeline = (TableLayout) findViewById(R.id.tbl_timeline);
        mTimeline.setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(CHARACTER_EXTRA) && getSelectedCharacter() == null)
            setAndShowCharacter(savedInstanceState.getString(CHARACTER_EXTRA));
        if (savedInstanceState.containsKey(KD_MOVE_EXTRA) && getSelectedKDMove() == null)
            setAndShowKDMove(savedInstanceState.getString(KD_MOVE_EXTRA));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMainMenuPresenter.handleResult(requestCode, resultCode, data);
        // If an orientation change occurs,
        // Character and KD Move are null after coming out of method for no reason!
        // Even though during the method, they are verified as being set! WHAT?
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Select screens should have sent info back by this point. (if they started)

        // start the presenter if it isn't already starting
        //if (!mMainMenuPresenter.isStarting())
            mMainMenuPresenter.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the user's selections
        //  (i.e. on orientation change, etc.)
        String character = getSelectedCharacter();
        if (character != null)
            outState.putString(CHARACTER_EXTRA, character);

        String kdMove = getSelectedKDMove();
        if (kdMove != null)
            outState.putString(KD_MOVE_EXTRA, kdMove);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /*------------------------*\
    * View Interface Functions *
    \*------------------------*/

    @Override
    public void setPresenter(MainMenuContract.Presenter presenter) {
        if (mMainMenuPresenter == null)
            mMainMenuPresenter = presenter;
    }

    @Override
    public boolean hasSelectedCharacter() {
        return getSelectedCharacter() != null;
    }

    @Override
    public boolean hasSelectedKDMove() {
        return getSelectedKDMove() != null;
    }

    /**
     * Character Select starts here...
     */
    @Override
    public void showCharacterSelect() {
        Intent intent = new Intent(getApplicationContext(), CharacterSelectActivity.class);

        startActivityForResult(intent, CHAR_SEL_REQUEST_CODE);
    }

    /**
     * Interface method for setting the Character. Allows the Presenter to tell the View what to set.
     * @param character Character's 3-letter code for accessing its database table
     */
    @Override
    public void setAndShowCharacter(String character) {
        setSelectedCharacter(character);
        // temp
        ((TextView) findViewById(R.id.tv_temp)).setText(getSelectedCharacter());
    }

    @Override
    public void showKDMoveSelect() {
        Intent intent = new Intent(getApplicationContext(), KDMoveSelectActivity.class);
        // pass the character code to the activity so its presenter can query the database
        intent.putExtra(CHARACTER_EXTRA, getSelectedCharacter());
        // start the KDMoveSelectActivity
        startActivityForResult(intent, KD_MOVE_SEL_REQUEST_CODE);
    }

    /**
     * Interface method for setting the KD Move. Allows the Presenter to tell the View what to set.
     * @param kdMove Name of the Move
     */
    @Override
    public void setAndShowKDMove(String kdMove) {
        setSelectedKDMove(kdMove);

        //temp
        String tvText = ((TextView) findViewById(R.id.tv_temp)).getText().toString();
        tvText = tvText + "\n" + getSelectedKDMove();
        ((TextView) findViewById(R.id.tv_temp)).setText(tvText);
    }

    /**
     * ViewStub implementation doesn't work when orientation is changed. Maybe even on configuration change?
     * Shows timeline if hidden.
     */
    @Override
    public void showTimeline() {
//        ViewStub vs = (ViewStub) findViewById(R.id.viewStub_timeline);
//        if (vs != null)
//            mTimeline = (TableLayout) ( vs.inflate() );
//        else {
//            mTimeline = (TableLayout) findViewById(R.id.tbl_timeline);
//        }
        if (mTimeline != null && mTimeline.getVisibility() == View.INVISIBLE)
            mTimeline.setVisibility(View.VISIBLE);
    }


    /*-----------------*\
    * Getters / Setters *
    \*-----------------*/

    /**
     * @return {@link MainActivity#mSelectedCharacter}
     */
    public String getSelectedCharacter() {
        return mSelectedCharacter;
    }

    public void setSelectedCharacter(String character) {
        mSelectedCharacter = character;
    }

    public String getSelectedKDMove() {
        return mSelectedKDMove;
    }

    public void setSelectedKDMove(String kdMove) {
        mSelectedKDMove = kdMove;
    }
}
