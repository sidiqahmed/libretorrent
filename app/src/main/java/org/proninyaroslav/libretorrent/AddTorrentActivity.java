/*
 * Copyright (C) 2016, 2017 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of LibreTorrent.
 *
 * LibreTorrent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibreTorrent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibreTorrent.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.proninyaroslav.libretorrent;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.proninyaroslav.libretorrent.core.utils.Utils;
import org.proninyaroslav.libretorrent.dialogs.SpinnerProgressDialog;
import org.proninyaroslav.libretorrent.fragments.AddTorrentFragment;
import org.proninyaroslav.libretorrent.fragments.FragmentCallback;
import org.proninyaroslav.libretorrent.services.TorrentTaskService;

/*
 * The dialog for adding torrent. The parent window.
 */

public class AddTorrentActivity extends AppCompatActivity
        implements
        FragmentCallback,
        AddTorrentFragment.Callback
{

    @SuppressWarnings("unused")
    private static final String TAG = AddTorrentActivity.class.getSimpleName();

    private static final String TAG_SPINNER_PROGRESS = "spinner_progress";

    public static final String TAG_URI = "uri";
    public static final String TAG_RESULT_TORRENT = "result_bundle";

    private AddTorrentFragment addTorrentFragment;
    private SpinnerProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (Utils.isDarkTheme(getApplicationContext())) {
            setTheme(R.style.AppTheme_Dark);
        }
        else if(Utils.isBlackTheme(getApplicationContext())) {
            setTheme(R.style.AppTheme_Black);
        }

        setContentView(R.layout.activity_add_torrent);

        addTorrentFragment = (AddTorrentFragment) getFragmentManager()
                .findFragmentById(R.id.add_torrent_fragmentContainer);

        startService(new Intent(getApplicationContext(), TorrentTaskService.class));

        Intent intent = getIntent();
        Uri uri;
        if (intent.getData() != null) {
            /* Implicit intent with path to torrent file, http or magnet link */
            uri = intent.getData();
        } else {
            uri = intent.getParcelableExtra(TAG_URI);
        }

        if (uri != null) {
            addTorrentFragment.setUri(uri);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        progress = (SpinnerProgressDialog) getFragmentManager().findFragmentByTag(TAG_SPINNER_PROGRESS);
    }

    @Override
    public void onPreExecute(String progressDialogText)
    {
        showProgress(progressDialogText);
    }

    @Override
    public void onPostExecute()
    {
        dismissProgress();
    }

    private void showProgress(String progressDialogText)
    {
        progress = SpinnerProgressDialog.newInstance(
                R.string.decode_torrent_progress_title,
                progressDialogText,
                0,
                true,
                true);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(progress, TAG_SPINNER_PROGRESS);
        ft.commitAllowingStateLoss();
    }

    private void dismissProgress()
    {
        if (progress != null) {
            try {
                progress.dismiss();
            } catch (Exception e) {
                /* Ignore */
            }
        }

        progress = null;
    }

    @Override
    public void fragmentFinished(Intent intent, ResultCode code)
    {
        if (code == ResultCode.OK) {
            /* If add torrent dialog has been called by an implicit intent */
            if (getIntent().getData() != null && intent.hasExtra(TAG_RESULT_TORRENT)) {
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(TAG_RESULT_TORRENT, intent.getParcelableExtra(TAG_RESULT_TORRENT));
                startActivity(i);
            } else {
                setResult(RESULT_OK, intent);
            }

        } else if (code == ResultCode.BACK) {
            /* For correctly finishing activity, if it was called by implicit intent */
            if (getIntent().getData() != null) {
                finish();
            } else {
                setResult(RESULT_CANCELED, intent);
            }

        } else if (code == ResultCode.CANCEL) {
            setResult(RESULT_CANCELED, intent);
        }

        finish();
    }

    @Override
    public void onBackPressed()
    {
        addTorrentFragment.onBackPressed();
    }
}
