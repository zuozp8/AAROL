package iswd.aarol.widget;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import iswd.aarol.R;
import iswd.aarol.model.LocationPackageSnippet;
import iswd.aarol.model.PackageManager;

public class PackagesArrayAdapter extends ArrayAdapter<LocationPackageSnippet> {

    private LayoutInflater mInflater;

    public PackagesArrayAdapter(Context context, List<LocationPackageSnippet> list) {
        super(context, 0, list);

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.packages_item, parent, false);
        } else {
            view = convertView;
        }
        LocationPackageSnippet item = getItem(position);
        TextView textView = (TextView) view.findViewById(R.id.packageNameText);
        textView.setText(Html.fromHtml(
                "<big><b>" + item.name + "</b></big> "
                        + getContext().getResources().getString(R.string.packege_by) + " " + item.creatorName + "<br>"
                        + "<small><i>" + item.description + "</i><small>"));

        boolean downloaded = PackageManager.isDownloaded(getContext(), item.name);
        view.findViewById(R.id.downloadButton).setVisibility(downloaded ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.enableCheckBox).setVisibility(downloaded ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.detailsButton).setVisibility(downloaded ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.deleteButton).setVisibility(downloaded ? View.VISIBLE : View.GONE);

        CheckBox checkBox = (CheckBox) view.findViewById(R.id.enableCheckBox);
        checkBox.setChecked(PackageManager.isEnabled(getContext(), item.name));
        view.setTag(R.id.tag_package_name, item.name);
        return view;
    }
}
