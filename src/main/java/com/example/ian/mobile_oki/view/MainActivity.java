package com.example.ian.mobile_oki.view;

import android.app.DialogFragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannedString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ian.mobile_oki.OkiApp;
import com.example.ian.mobile_oki.R;
import com.example.ian.mobile_oki.contracts.MainMenuContract;
import com.example.ian.mobile_oki.data.OkiMoveListItem;
import com.example.ian.mobile_oki.databinding.TimelineBodyRowBinding;
import com.example.ian.mobile_oki.logic.MainMenuPresenter;
import com.example.ian.mobile_oki.util.EMenuItem;
import com.example.ian.mobile_oki.util.OkiUtil;

import java.util.ArrayList;

import static android.graphics.Color.TRANSPARENT;

/**
 * The Timeline Activity / Main Menu of the <b>Mobile Oki Calculator</b> app.
 * <p/>
 * <i>Shortened the name to MOKI, since I had to make another Git repo.</i>
 **/
public class MainActivity extends AppCompatActivity
        implements MainMenuContract.View,
        ClearAllOkiSlotsDialogFragment.DialogListener {

//    public static final String EXTRA_MESSAGE = "com.example.ian.MESSAGE";
    public static final int CHAR_SEL_REQUEST_CODE = 6969;
    public static final int KD_MOVE_SEL_REQUEST_CODE = 8008;
    public static final int OKI_MOVE_SEL_REQUEST_CODE = 7175;
    public static final int LOAD_ACTIVITY_REQUEST_CODE = 420;

    public static final int MAX_TIMELINE_FRAMES = 120;

    private static final String TAG = MainActivity.class.getSimpleName();

    private MainMenuContract.Presenter mMainMenuPresenter;

    private TableLayout mTimeline;

    private ActionBar mActionBar;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavDrawer;

    private MenuItem mOkiDrawerButton;

     /** Gives access to the generated Data Binding class for the timeline's body */
    TimelineBodyRowBinding mBodyBinding;

    private ArrayList<TextView> mOkiColumns;

    private TextView mFkKD;
    private TextView mFkKDR;
    private TextView mFkKDBR;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();

        setupNavDrawer();
        setupOkiDrawer();

        // get or create presenter instance, which will in turn set this view's presenter
        setPresenter((MainMenuPresenter) getLastCustomNonConfigurationInstance());

        mTimeline = (TableLayout) findViewById(R.id.tbl_timeline);

        bindTimelineBody();
        storeOkiColumns();
        setupRowSelector();

        mBodyBinding.tvBodyFramesTens.setHorizontallyScrolling(true); // allows tens-digit col to have double digits on one row

        setHeaderClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMainMenuPresenter.handleResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String character = mMainMenuPresenter.getCurrentCharacter(true);
        if (mActionBar != null && character != null)
            mActionBar.setTitle(character);

        hideTimeline();
        updateCurrentOkiDrawer();

        mMainMenuPresenter.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mMainMenuPresenter;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mMainMenuPresenter.closeStorageDb();
        mMainMenuPresenter.detachView();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        // open nav drawer on Timeline screen
        else if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) { // drawer is closed
            mDrawerLayout.openDrawer(GravityCompat.START);      // open drawer
            return;
        } else { // show prompt, exit if pressed again
            if(mToast != null && mToast.getView().getTag() == "exit-toast" &&
                    mToast.getView().getWindowVisibility() == View.VISIBLE) {

                mToast.cancel();
                finish();
            } else {
                mToast = Toast.makeText(OkiApp.getContext(), "Press again to exit...", Toast.LENGTH_SHORT);
                mToast.getView().setTag("exit-toast");
                mToast.show();
                return;
            }
        }

        // default behavior
        super.onBackPressed();
    }

    /*------------------------*\
    * View Interface Functions *
    \*------------------------*/

    @Override
    public void setPresenter(MainMenuContract.Presenter presenter) {

        if (presenter == null)
            mMainMenuPresenter = MainMenuPresenter.newInstance(this);
        else
            mMainMenuPresenter = presenter;

        mMainMenuPresenter.attachView(this);
    }

    @Override
    public boolean hasSelectedCharacter() {
        return mMainMenuPresenter.getCurrentCharacter(false) != null;
    }

    @Override
    public boolean  hasSelectedKDMove() {
        return mMainMenuPresenter.getCurrentKDMove() != null;
    }

    @Override
    public void showCharacterSelect() {
        Intent intent = new Intent(OkiApp.getContext(), CharacterSelectActivity.class);

        startActivityForResult(intent, CHAR_SEL_REQUEST_CODE);
    }

    @Override
    public void showKDMoveSelect() {
        Intent intent = new Intent(OkiApp.getContext(), KDMoveSelectActivity.class);
        // start the KDMoveSelectActivity
        startActivityForResult(intent, KD_MOVE_SEL_REQUEST_CODE);
    }

    @Override
    public void showOkiMoveSelect() {
        Intent intent = new Intent(OkiApp.getContext(), OkiMoveSelectActivity.class);
        // start the OkiMoveSelectActivity
        startActivityForResult(intent, OKI_MOVE_SEL_REQUEST_CODE);
    }

    /**
     * Show the Load Setup screen.
     */
    @Override
    public void showLoadActivity() {
        Intent intent = new Intent(OkiApp.getContext(), LoadActivity.class);

        startActivityForResult(intent, LOAD_ACTIVITY_REQUEST_CODE);
    }

    /**
     * Shows timeline if hidden, and refreshes its visuals.<br/>
     */
    @Override
    public void showTimeline() {
        if (mTimeline != null) {
            if (mTimeline.getVisibility() != View.VISIBLE)
                mTimeline.setVisibility(View.VISIBLE);

            // update columns
            updateKDAColumns();
            updateAllOkiColumns();
            updateOkiSlotColor(mMainMenuPresenter.getCurrentOkiSlot());
            updateCurrentOkiDrawer();

            // update the actionbar to show the menu now that the timeline is visible
            invalidateOptionsMenu();
        }
    }

    @Override
    public void hideTimeline() {
        mTimeline.setVisibility(View.INVISIBLE);
    }

    /**
     * Update the Timeline content of all Oki Slots.
     */
    @Override
    public void updateAllOkiColumns() {
        Log.d(TAG, "updateAllOkiColumns: updating!");
        for (int slot = 1; slot <= 7; slot++){
            updateOkiColumn(slot, false);
        }
    }

    /**
     * Update the Timeline with the frame data of the specified Oki Slot's Oki Move.<br/>
     * <br/>
     * <i>Sets the text of the specified Oki Slot.</i>
     * @param okiSlot       The number of the Oki Slot into which the content will be inserted.
     * @param useCurrentRow Specify whether to use either the currently selected row (true) or
     *                      the row that is already being used at that Oki Slot (false).
     */
    @Override
    public void updateOkiColumn(int okiSlot, boolean useCurrentRow) {
        TextView column = mOkiColumns.get(okiSlot - 1);

        column.setText(mMainMenuPresenter.getOkiColumnContent(okiSlot, useCurrentRow));
    }

    @Override
    public void updateCurrentOkiDrawer(){
        OkiMoveListItem okiMove;
        String okiMoveName;
        ScrollView currentOkiDrawer = (ScrollView) findViewById(R.id.sv_currentoki_drawer);
        ArrayList<TextView> moves = new ArrayList<>();

        moves.add(0, (TextView) currentOkiDrawer.findViewById(R.id.tv_kd_item));
        moves.add(1, (TextView) currentOkiDrawer.findViewById(R.id.tv_oki1_item));
        moves.add(2, (TextView) currentOkiDrawer.findViewById(R.id.tv_oki2_item));
        moves.add(3, (TextView) currentOkiDrawer.findViewById(R.id.tv_oki3_item));
        moves.add(4, (TextView) currentOkiDrawer.findViewById(R.id.tv_oki4_item));
        moves.add(5, (TextView) currentOkiDrawer.findViewById(R.id.tv_oki5_item));
        moves.add(6, (TextView) currentOkiDrawer.findViewById(R.id.tv_oki6_item));
        moves.add(7, (TextView) currentOkiDrawer.findViewById(R.id.tv_oki7_item));

        mFkKD = (TextView) currentOkiDrawer.findViewById(R.id.tv_fk_kd);
        mFkKDR = (TextView) currentOkiDrawer.findViewById(R.id.tv_fk_kdr);
        mFkKDBR = (TextView) currentOkiDrawer.findViewById(R.id.tv_fk_kdbr);

        if (mMainMenuPresenter.getCurrentKDMove() != null)
            moves.get(0).setText(mMainMenuPresenter.getCurrentKDMove());
        else moves.get(0).setText("");

        updateFrameKill();

        for (int i=1; i <= 7; i++) {
            okiMove = mMainMenuPresenter.getCurrentOkiMoveAt(i);
            if (okiMove != null) {
                okiMoveName = okiMove.getMove();
                moves.get(i).setText(okiMoveName);
            } else moves.get(i).setText("");
        }
    }

    private void updateFrameKill(){
        if (mMainMenuPresenter.getCurrentKDMove() != null) {
            // update Frame Kill
            mFkKD.setText(String.valueOf(mMainMenuPresenter.frameKillKD()));
            mFkKDR.setText(String.valueOf(mMainMenuPresenter.frameKillKDR()));
            mFkKDBR.setText(String.valueOf(mMainMenuPresenter.frameKillKDBR()));
        }
    }

    @Override
    public void showClearOkiSlotsDialogue() {
        ClearAllOkiSlotsDialogFragment dialogue = new ClearAllOkiSlotsDialogFragment();
        dialogue.show(getFragmentManager(), "clear-oki-slots-dialog");
    }

    @Override
    public void setCharacterWarningVisible(boolean shouldBeVisible) {
        TextView warning = (TextView) findViewById(R.id.tv_warning_no_char);
        if (shouldBeVisible)
            warning.setVisibility(View.VISIBLE);
        else
            warning.setVisibility(View.GONE);
    }

    @Override
    public void setKDWarningVisible(boolean shouldBeVisible) {
        TextView warning = (TextView) findViewById(R.id.tv_warning_no_kd);
        if (shouldBeVisible)
            warning.setVisibility(View.VISIBLE);
        else
            warning.setVisibility(View.GONE);
    }


    /*---------------------------------*\
    * Timeline Updaters (Non-Interface) *
    \*---------------------------------*/

    private void updateKDAColumns(){
        // get formatted text from presenter
        // (SpannedStrings allow multiple colors and styles in one TextView)
        SpannedString[] formattedTextValues = mMainMenuPresenter.getKDAColumnContent();

        mBodyBinding.tvBodyKd.setText(formattedTextValues[0]);
        mBodyBinding.tvBodyKdr.setText(formattedTextValues[1]);
        mBodyBinding.tvBodyKdbr.setText(formattedTextValues[2]);
    }

    private void updateOkiSlotColor(int okiSlot) {
        TextView okiBody, okiHeader;
        int currentOkiSlot = mMainMenuPresenter.getCurrentOkiSlot();
        // If a slot is already selected...
        if (currentOkiSlot > 0 && currentOkiSlot < 8) {
            // set "selected" header and body column to "unselected"
            // Find oki slot header and body, and reset background color
            okiHeader = (TextView) findViewById(R.id.tr_header)
                    .findViewWithTag(String.valueOf(currentOkiSlot));
            okiHeader.setBackgroundColor(OkiUtil.getColor(R.color.bgTableOKI));
            // Find oki slot body and reset background color
            okiBody = (TextView) findViewById(R.id.tr_body)
                    .findViewWithTag(String.valueOf(currentOkiSlot));
            okiBody.setBackgroundColor(OkiUtil.getColor(R.color.bgTableOKI));
        }
        // set "unselected" header and body column color to "selected"
        okiHeader = (TextView) findViewById(R.id.tr_header)
                .findViewWithTag(String.valueOf(okiSlot));
        okiHeader.setBackgroundColor(OkiUtil.getColor(R.color.colorPrimaryDark));

        okiBody = (TextView) findViewById(R.id.tr_body)
                .findViewWithTag(String.valueOf(okiSlot));
        okiBody.setBackgroundColor(OkiUtil.getColor(R.color.colorPrimaryDark));
    }

    private void updateRowColor(){
        // get view of current row (in ListView)
        int rowIndex = mMainMenuPresenter.getCurrentRow() - 1;

        TextView rowView = (TextView) getRowView(rowIndex);

        rowView.setBackgroundColor(OkiUtil.getColor(R.color.secLight));
        rowView.getBackground().setAlpha(50);
    }


    /*----*\
    * Misc *
    \*----*/

    private void showOkiSlotWarning() {
        if (mToast != null) mToast.cancel();

        mToast = Toast.makeText(this, "Select an OKI# first!", Toast.LENGTH_LONG);
        mToast.show();
    }

    public void onHeaderClick(View view){
        int okiSlotNumber = Integer.valueOf(view.getTag().toString());

        if (okiSlotNumber != mMainMenuPresenter.getCurrentOkiSlot()) {
            updateOkiSlotColor(okiSlotNumber); // called before setting to reset the old slot's color
            mMainMenuPresenter.setCurrentOkiSlot(okiSlotNumber);
        }
        else // clicking header when already selected opens oki select screen
            showOkiMoveSelect();
    }

    private void setHeaderClickListeners() {
        TableRow headerRow = (TableRow) mTimeline.findViewById(R.id.tr_header);

        // return, if the listeners are already set (setting a listener makes isLongClickable true)
        if (headerRow.findViewById(R.id.tv_header_oki_7).isLongClickable()) return;

        ArrayList<TextView> headers = new ArrayList<>(7);

        headers.add((TextView) headerRow.findViewById(R.id.tv_header_oki_1));
        headers.add((TextView) headerRow.findViewById(R.id.tv_header_oki_2));
        headers.add((TextView) headerRow.findViewById(R.id.tv_header_oki_3));
        headers.add((TextView) headerRow.findViewById(R.id.tv_header_oki_4));
        headers.add((TextView) headerRow.findViewById(R.id.tv_header_oki_5));
        headers.add((TextView) headerRow.findViewById(R.id.tv_header_oki_6));
        headers.add((TextView) headerRow.findViewById(R.id.tv_header_oki_7));

        for (int i = 0; i < 7; i++){
            headers.get(i).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    int currentOkiSlot = mMainMenuPresenter.getCurrentOkiSlot(),
                            slotToClear = Integer.valueOf(view.getTag().toString());
                    // temporarily set the current oki slot to the slot which will be cleared
                    mMainMenuPresenter.setCurrentOkiSlot(slotToClear);
                    mMainMenuPresenter.clearCurrentOkiSlot();
                    // restore the current oki slot
                    mMainMenuPresenter.setCurrentOkiSlot(currentOkiSlot);

                    showOkiSlotCleared(slotToClear);

                    return true;
                }
            });
        }
    }

    private void showOkiSlotCleared(int clearedSlot) {
        if (mToast != null && mToast.getView().getWindowVisibility() == View.VISIBLE)
            mToast.cancel();

        String message = "Oki Slot " + clearedSlot + " cleared!";

        mToast = Toast.makeText(OkiApp.getContext(), message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void bindTimelineBody() {
        View body = mTimeline.findViewById(R.id.tr_body);

        mBodyBinding = DataBindingUtil.bind(body);
    }

    /**
     * Store the Oki Slot columns (Views/widgets) in an array. (does not store content)
     */
    private void storeOkiColumns() {
        mOkiColumns = new ArrayList<>();
        mOkiColumns.add(mBodyBinding.tvBodyOki1);
        mOkiColumns.add(mBodyBinding.tvBodyOki2);
        mOkiColumns.add(mBodyBinding.tvBodyOki3);
        mOkiColumns.add(mBodyBinding.tvBodyOki4);
        mOkiColumns.add(mBodyBinding.tvBodyOki5);
        mOkiColumns.add(mBodyBinding.tvBodyOki6);
        mOkiColumns.add(mBodyBinding.tvBodyOki7);
    }

    private void setupRowSelector() {
        mBodyBinding.lvRowSelector.setDivider(null);
        mBodyBinding.lvRowSelector.setDividerHeight(0);
        // make rows
        ArrayList<String> rows = new ArrayList<>(MAX_TIMELINE_FRAMES);
        for (int i = 0; i < MAX_TIMELINE_FRAMES; i++){
            rows.add(" ");
        }
        // set adapter
        mBodyBinding.lvRowSelector.setAdapter(new ArrayAdapter<>(this, R.layout.row_selector_item, rows));
        mBodyBinding.lvRowSelector.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                setCurrentRow(i + 1, view);
            }
        });
        // ListView children aren't accessible until it's visible, so create a listener for when it becomes visible.
        mBodyBinding.lvRowSelector.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateRowColor();

                mBodyBinding.lvRowSelector.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }


    /*-----------------*\
    * Getters / Setters *
    \*-----------------*/

    private void setCurrentRow(int okiRow, View view) {
        int previousRowNumber = mMainMenuPresenter.getCurrentRow();

        if (okiRow != mMainMenuPresenter.getCurrentRow()) {
            mMainMenuPresenter.setCurrentRow(okiRow);

            updateFrameKill();
        } else {
            // set current row as oki row
            mMainMenuPresenter.moveOkiMove();
            updateOkiColumn(mMainMenuPresenter.getCurrentOkiSlot(), false);
        }

        // If what I've read is right, ListView children are null unless visible.
        if (mBodyBinding.lvRowSelector.getVisibility() != View.VISIBLE) return;

        setRowColor(previousRowNumber, view);
    }

    private void setRowColor(int oldOkiRow, View view) {
        // reset previous row color
        mBodyBinding.lvRowSelector
                .getChildAt(oldOkiRow - 1)
                .setBackgroundColor(TRANSPARENT);
        // set current row color
        view.setBackgroundColor(OkiUtil.getColor(R.color.secLight));
        view.getBackground().setAlpha(50);
    }

    private View getRowView(int rowIndex){
        // if row is not on-screen...
        if (rowIndex < mBodyBinding.lvRowSelector.getFirstVisiblePosition() ||
            rowIndex > mBodyBinding.lvRowSelector.getLastVisiblePosition() )
            return mBodyBinding.lvRowSelector.getAdapter()
                    .getView(rowIndex, null, mBodyBinding.lvRowSelector);
        else // row is on-screen
            return mBodyBinding.lvRowSelector.getChildAt(
                    rowIndex - mBodyBinding.lvRowSelector.getFirstVisiblePosition());
    }


    /*-------------------*\
    * Nav Drawer and Menu *
    \*-------------------*/

    private void setupNavDrawer() {
//        DisplayMetrics metrics = OkiApp.getContext().getResources().getDisplayMetrics();
//        float displayWidth = metrics.widthPixels / metrics.density;

        String[] menuItems = getResources().getStringArray(R.array.nav_menu_items);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.dl_nav_drawerlayout);
        mNavDrawer = (NavigationView) mDrawerLayout.findViewById(R.id.nav_drawer);
        ListView navDrawerList = (ListView) findViewById(R.id.lv_nav_menu);
//        mNavDrawerList.setMinimumWidth(
//                (int) Math.min(R.dimen.drawer_max_width, displayWidth - R.attr.actionBarSize));

        // set list adapter
        navDrawerList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_selectable_list_item, menuItems));
        navDrawerList.setDivider(null);

        // set list click listener
        navDrawerList.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        // make drawer toggleable
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            // Called when a drawer finishes closing.
            @Override
            public void onDrawerClosed(View drawerView) {
                if(drawerView == mNavDrawer) {
                    super.onDrawerClosed(drawerView);
//                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu() and onCreateOptionsMenu()
                }
            }

            // Called when a drawer finishes opening.
            @Override
            public void onDrawerOpened(View drawerView) {
                if(drawerView == mNavDrawer) {
                    super.onDrawerOpened(drawerView);
//                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu() and onCreateOptionsMenu()
                }
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if(drawerView == mNavDrawer)
                    super.onDrawerSlide(drawerView, slideOffset);
            }
        };

        // add drawer toggle listener
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupOkiDrawer() {

        final ScrollView okiDrawer = (ScrollView) mDrawerLayout.findViewById(R.id.sv_currentoki_drawer);

        // make oki setup drawer toggleable
        DrawerLayout.DrawerListener mOkiDrawerToggleListener = new DrawerLayout.DrawerListener(){

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                if (drawerView == okiDrawer)
                    syncOkiDrawerState();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                if (drawerView == okiDrawer)
                    syncOkiDrawerState();
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        };

        mDrawerLayout.addDrawerListener(mOkiDrawerToggleListener);
    }

    private void selectItem(int id) {
        EMenuItem item = EMenuItem.values()[id];

        // open corresponding activity
        switch (item) {
            case MENU_CHAR_SELECT:
                showCharacterSelect();
                break;
            case MENU_KD_SELECT:
                if(hasSelectedCharacter())
                    showKDMoveSelect();
                break;
            case MENU_OKI_SELECT:
                if(hasSelectedCharacter() && hasSelectedKDMove()) {
                    int currentOkiSlot = mMainMenuPresenter.getCurrentOkiSlot();
                    if (currentOkiSlot > 0 && currentOkiSlot < 8) // If a slot is selected...
                        showOkiMoveSelect();
                    else
                        showOkiSlotWarning();
                }
                break;
            case MENU_SAVE:
                if(!mMainMenuPresenter.isTimelineBlank()){
                    // save
                    if (mMainMenuPresenter.saveData()) {
                        if (mToast != null) mToast.cancel();

                        mToast = Toast.makeText(OkiApp.getContext(),
                                "Saved successfully!", Toast.LENGTH_SHORT);
                        mToast.show();
                    }
                }
                break;
            case MENU_LOAD:
                // launch 'Load' activity
                showLoadActivity();
                break;
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.timeline_options_menu, menu);

        mOkiDrawerButton = menu.findItem(R.id.timeline_rightDrawer_btn);

        syncOkiDrawerState();

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //boolean drawerOpen = mDrawerLayout.isDrawerOpen(mNavDrawerList);
        // Hide items in action bar if unrelated to nav menu
          // TODO: implement, if/when actionbar items are added... (and if nav bar covers actionbar)

        // only show menu options related to the Timeline if the Timeline is visible
        boolean isVisible = mMainMenuPresenter.isTimelineReady();

        menu.findItem(R.id.timeline_clear_selected).setVisible(isVisible);
        menu.findItem(R.id.timeline_clear_all     ).setVisible(isVisible);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // drawer toggle selected
        if (mDrawerToggle.onOptionsItemSelected(item))
        { // close right drawer if open
            if(mDrawerLayout.isDrawerVisible(GravityCompat.END))
                mDrawerLayout.closeDrawer(GravityCompat.END);

            return true;
        }

        // handle other actionbar items selected
        switch (item.getItemId()) {
            case R.id.timeline_rightDrawer_btn:
                toggleOkiDrawer();
                return true;
            case R.id.timeline_clear_selected:
                mMainMenuPresenter.clearCurrentOkiSlot();
                showOkiSlotCleared(mMainMenuPresenter.getCurrentOkiSlot());
                return true;
            case R.id.timeline_clear_all:
                showClearOkiSlotsDialogue();
                return true;
        }

        // default
        return false;
    }

    private void toggleOkiDrawer() {
        // close left drawer if open
        if (mDrawerLayout.isDrawerVisible(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);

        // close right drawer if open
        if (mDrawerLayout.isDrawerVisible(GravityCompat.END))
            mDrawerLayout.closeDrawer(GravityCompat.END);
        else
            mDrawerLayout.openDrawer(GravityCompat.END);
    }

    private void syncOkiDrawerState() {
        mOkiDrawerButton.setIcon(mDrawerLayout.isDrawerOpen(GravityCompat.END) ?
                 R.drawable.information : R.drawable.information_outline);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // sync toggle state (open/closed)
        mDrawerToggle.syncState();
    }


    /*------------------------*\
    * Dialog Interface Methods *
    \*------------------------*/

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        mMainMenuPresenter.clearAllOkiSlots();
        dialog.dismiss();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
    }
}