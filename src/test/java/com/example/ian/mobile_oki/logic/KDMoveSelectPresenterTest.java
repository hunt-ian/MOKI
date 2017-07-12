package com.example.ian.mobile_oki.logic;

import com.example.ian.mobile_oki.contracts.KDMoveSelectContract;
import com.example.ian.mobile_oki.data.DatabaseInterface;
import com.example.ian.mobile_oki.data.KDMoveListItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests Knockdown-Move List Presenter
 * Created by Ian on 7/7/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class KDMoveSelectPresenterTest {

    @Mock
    KDMoveSelectContract.View mView;

    @Mock
    DatabaseInterface mDB;

    KDMoveSelectContract.Presenter mPresenter;

    @Before
    public void setUp() {
        initMocks(this);
        mPresenter = new KDMoveSelectPresenter(mView, mDB);
    }

    @Test
    public void testStart() {
        ArgumentCaptor<KDMoveSelectContract.Presenter> presenterArgumentCaptor =
                ArgumentCaptor.forClass(KDMoveSelectPresenter.class);

        mPresenter.start();

        verify(mView).setPresenter(presenterArgumentCaptor.capture());

        assertThat(presenterArgumentCaptor.getValue(), equalTo(mPresenter));
    }

    /**
     * List should contain
     * <ol>
     *     <li>Move Name</li>
     *     <li>KDA</li>
     *     <li>KDRA</li>
     *     <li>KDBRA</li>
     *     <li>Startup</li>
     *     <li>Active</li>
     *     <li>Recovery</li>
     * </ol>
     */
    @Test
    public void shouldFetchListOfKDMoves() {
        // Given a list of moves, with and without kd property
        List<KDMoveListItem> pseudoListOfMoves = new ArrayList<>();
        pseudoListOfMoves.add(new KDMoveListItem("move1",
                90,30,35,
                4,2,7));
        pseudoListOfMoves.add(new KDMoveListItem("move2",
                80, 20, 25,
                8,3,18));

        // When
        String s = anyString();
        when(mDB.getKDMoves(s)).thenReturn(pseudoListOfMoves);

        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
//        ArgumentCaptor<KDMoveListItem> argumentCaptor = ArgumentCaptor.forClass(KDMoveListItem.class);
        mPresenter.start();
        // Then
        verify(mView).displayKDMoveList();

        mPresenter.getListOfKDMoves(s);

        verify(mView).cacheKDMoveList(listArgumentCaptor.capture());
        assertEquals(2, listArgumentCaptor.getValue().size());
    }

    /**
     * User clicked an item...
     */
    @Test
    public void onListItemClicked_shouldInstructViewToFinish(){
        //User clicks an item, View notifies the Presenter

    }
}