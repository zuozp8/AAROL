package iswd.aarol.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import iswd.aarol.R;
import iswd.aarol.model.LocationPoint;

public class LocationsArrayAdapter extends ArrayAdapter<LocationPoint> {

    private LayoutInflater mInflater;

    public LocationsArrayAdapter(Context context, List<LocationPoint> list) {
        super(context, 0, list);

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.locations_item, parent, false);
        } else {
            view = convertView;
        }
        LocationPoint item = getItem(position);

        TextView nameTextView = (TextView) view.findViewById(R.id.locationNameText);
        nameTextView.setText(item.getName());

        TextView wikipediaLinkTextView = (TextView) view.findViewById(R.id.wikipediaLink);
        if (item.getWikipediaLink() != null && !item.getWikipediaLink().isEmpty()) {
            wikipediaLinkTextView.setText(item.getWikipediaLink());
        } else {
            wikipediaLinkTextView.setVisibility(View.GONE);
        }

        view.findViewById(R.id.leadToButton).setTag(R.id.tag_location, position);
        return view;
    }
}
