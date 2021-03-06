package iswd.aarol.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import com.larswerkman.holocolorpicker.ColorPicker;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import iswd.aarol.R;
import iswd.aarol.model.PackageManager;
import iswd.aarol.model.Repository;
import iswd.aarol.model.RepositoryFactory;
import iswd.aarol.widget.PackagesArrayAdapter;

public class PackagesActivity extends Activity {

    enum DownloadStatus {
        SUCCESS(R.string.package_download_success),
        PARSING_ERROR(R.string.package_download_parsing_error),
        HTTP_ERROR(R.string.package_download_http_error),
        NO_CONNECTION(R.string.package_download_no_connection);

        private int messageId;

        DownloadStatus(int messageId) {
            this.messageId = messageId;
        }

        public int getMessageId() {
            return messageId;
        }
    }

    ProgressDialog waitingDialog = null;
    AsyncTask<Void, Void, DownloadStatus> downloadingTask = null;
    Repository repository = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packages);
    }

    @Override
    protected void onResume() {
        super.onResume();

        waitingDialog = new ProgressDialog(this);
        String message = getResources().getString(R.string.updating_repository);
        waitingDialog.setMessage(message);
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);
        waitingDialog.show();

        updateRepository(); //Performs background task
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (downloadingTask != null) {
            downloadingTask.cancel(true);
        }
    }

    private void updateRepository() {
        downloadingTask = new AsyncTask<Void, Void, DownloadStatus>() {
            @Override
            protected DownloadStatus doInBackground(Void... params) {
                try {
                    final URL repositoryURL = getRepositoryURL();
                    InputStream content = (InputStream) repositoryURL.getContent();
                    repository = RepositoryFactory.createFromXML(content);
                } catch (IOException e) {
                    return DownloadStatus.HTTP_ERROR;
                } catch (XmlPullParserException e) {
                    return DownloadStatus.PARSING_ERROR;
                }
                return DownloadStatus.SUCCESS;
            }

            @Override
            protected void onCancelled() {
                repository = null;
            }

            @Override
            protected void onPostExecute(DownloadStatus status) {
                waitingDialog.dismiss();
                waitingDialog = null;

                Toast toast = Toast.makeText(getApplicationContext(), status.getMessageId(), Toast.LENGTH_LONG);
                toast.show();

                if (status == DownloadStatus.SUCCESS) {
                    ListView listView = getViewListView();
                    listView.setAdapter(new PackagesArrayAdapter(PackagesActivity.this, repository));
                } else {
                    //TODO handle error
                }
            }
        };
        downloadingTask.execute();
    }

    public void downloadClicked(View view) {
        waitingDialog = new ProgressDialog(this);
        String message = getResources().getString(R.string.downloading_package);
        waitingDialog.setMessage(message);
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);
        waitingDialog.show();

        final String name = getPackageNameFromParentsTag(view);

        downloadingTask = new AsyncTask<Void, Void, DownloadStatus>() {
            @Override
            protected DownloadStatus doInBackground(Void... params) {
                try {
                    InputStream inputStream = (InputStream) getRepositoryURL(name).getContent();
                    FileOutputStream fileOutputStream = openFileOutput(PackageManager.getFileNameOfPackage(name), MODE_PRIVATE);
                    IOUtils.copy(inputStream, fileOutputStream);

                } catch (IOException e) {
                    return DownloadStatus.HTTP_ERROR;
                }
                return DownloadStatus.SUCCESS;
            }

            @Override
            protected void onCancelled() {
            }

            @Override
            protected void onPostExecute(DownloadStatus status) {
                waitingDialog.dismiss();
                waitingDialog = null;

                Toast toast = Toast.makeText(getApplicationContext(), status.getMessageId(), Toast.LENGTH_LONG);
                toast.show();
                refreshList();
            }
        };
        downloadingTask.execute();
    }

    private URL getRepositoryURL(String name) {
        try {
            return new URL(getRepositoryURL(), ("packages/" + name + ".xml").replace(" ", "%20"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private URL getRepositoryURL() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            return new URL(sharedPref.getString("repo_url", "").replace(" ", "%20"));
        } catch (Exception e) {
            return null;
        }
    }

    public void deleteClicked(View view) {
        String name = getPackageNameFromParentsTag(view);
        deleteFile(PackageManager.getFileNameOfPackage(name));
        PackageManager.setEnabled(this, name, false);
        refreshList();
    }


    public void enableClicked(View view) {
        String name = getPackageNameFromParentsTag(view);
        boolean enabled = ((CheckBox) view).isChecked();
        PackageManager.setEnabled(this, name, enabled);
    }

    public void detailsClicked(View view) {
        String name = getPackageNameFromParentsTag(view);
        Intent intent = new Intent(this, PackageDetailsActivity.class);
        intent.putExtra(PackageDetailsActivity.PACKAGE_NAME_EXTRA, name);
        startActivity(intent);
    }

    private String getPackageNameFromParentsTag(View view) {
        View parent = (View) view.getParent();
        return (String) parent.getTag(R.id.tag_package_name);
    }

    private ListView getViewListView() {
        return (ListView) findViewById(R.id.packagesList);
    }

    private void refreshList() {
        ((BaseAdapter) getViewListView().getAdapter()).notifyDataSetChanged();
    }

    public void colorClicked(View view) {
        final String packageName = getPackageNameFromParentsTag(view);

        final ColorPicker colorPicker = new ColorPicker(this);
        colorPicker.setColor(PackageManager.getColorOfPackage(this, packageName));
        colorPicker.setOldCenterColor(PackageManager.getColorOfPackage(this, packageName));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_colorpicker);
        builder.setPositiveButton(R.string.colorpicker_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PackageManager.setColorOfPackage(PackagesActivity.this, packageName, colorPicker.getColor());
                refreshList();
            }
        });
        builder.setNegativeButton(R.string.colorpicker_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.setView(colorPicker);
        dialog.show();
    }
}
