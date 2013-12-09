package com.google.appinventor.components.runtime;

import java.util.List;

import com.google.appinventor.components.runtime.util.AnimationUtil;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Activity that presents the list of entities from the SemanticWebListPicker
 * @see LinkedDataListPicker
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class SWListActivity extends ListActivity {

  /**
   * Represents a URI and its label for presentation in the
   * SWListActivity.
   * @author Evan W. Patton <ewpatton@gmail.com>
   *
   */
  public static class LabeledUri implements Parcelable {
    private final String label;
    private final String uri;
    /**
     * Create a new labelled URI for the given values.
     * @param label
     * @param uri
     */
    public LabeledUri(String label, String uri) {
      if(label == null) {
        throw new IllegalArgumentException("label cannot be null");
      }
      if(uri == null) {
        throw new IllegalArgumentException("uri cannot be null");
      }
      this.label = label;
      this.uri = uri;
    }
    protected LabeledUri(Parcel parcel) {
      this.label = parcel.readString();
      this.uri = parcel.readString();
    }
    @Override
    public String toString() {
      return this.label;
    }
    @Override
    public int describeContents() {
      return 0;
    }
    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
      arg0.writeString(label);
      arg0.writeString(uri);
    }
    public static final Parcelable.Creator<LabeledUri> CREATOR = new Parcelable.Creator<LabeledUri>() {
      public LabeledUri[] newArray(int arg0) {
        return new SWListActivity.LabeledUri[arg0];
      }
      public LabeledUri createFromParcel(Parcel arg0) {
        return new LabeledUri(arg0);
      }
    };
  }

  private String closeAnim = "";
  private List<LabeledUri> items;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent myIntent = getIntent();
    if (myIntent.hasExtra(LinkedDataListPicker.SWLIST_ACTIVITY_ANIM_TYPE)) {
      closeAnim = myIntent.getStringExtra(LinkedDataListPicker.SWLIST_ACTIVITY_ANIM_TYPE);
    }
    if (myIntent.hasExtra(LinkedDataListPicker.SWLIST_ACTIVITY_ARG_NAME)) {
      items = getIntent().getParcelableArrayListExtra(LinkedDataListPicker.SWLIST_ACTIVITY_ARG_NAME);
      setListAdapter(new ArrayAdapter<LabeledUri>(this, android.R.layout.simple_list_item_1, items));
      getListView().setTextFilterEnabled(true);
    } else {
      setResult(RESULT_CANCELED);
      finish();
      AnimationUtil.ApplyCloseScreenAnimation(this, closeAnim);
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      boolean handled = super.onKeyDown(keyCode, event);
      AnimationUtil.ApplyCloseScreenAnimation(this, closeAnim);
      return handled;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onListItemClick(ListView lv, View v, int position, long id) {
    Intent resultIntent = new Intent();
    LabeledUri selectedItem = (LabeledUri) getListView().getItemAtPosition(position);
    resultIntent.putExtra(LinkedDataListPicker.SWLIST_ACTIVITY_RESULT_LABEL, selectedItem.label);
    resultIntent.putExtra(LinkedDataListPicker.SWLIST_ACTIVITY_RESULT_URI, selectedItem.uri);
    resultIntent.putExtra(LinkedDataListPicker.SWLIST_ACTIVITY_RESULT_INDEX, position + 1);
    closeAnim = selectedItem.label;
    setResult(RESULT_OK, resultIntent);
    finish();
    AnimationUtil.ApplyCloseScreenAnimation(this, closeAnim);
  }

}
