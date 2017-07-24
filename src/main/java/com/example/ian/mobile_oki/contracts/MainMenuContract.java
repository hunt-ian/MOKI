package com.example.ian.mobile_oki.contracts;

import android.content.Intent;

import com.example.ian.mobile_oki.BasePresenter;
import com.example.ian.mobile_oki.BaseView;

/**
 * <p>Interface for Main Menu and MainMenuPresenter
 * <p>
 * Created by Ian on 7/2/2017.
 */

public interface MainMenuContract {

    interface View extends BaseView<Presenter>{

        boolean hasSelectedCharacter();

        boolean hasSelectedKDMove();

        void showCharacterSelect();

        void setAndShowCharacter(String character);

        void showKDMoveSelect();

        void setAndShowKDMove(String kdMove);

        void showTimeline();
    }

    interface Presenter extends BasePresenter {

        void handleResult(int requestCode, int resultCode, Intent intent);

        boolean isTimelineReady();

        boolean isStarting();
    }
}
