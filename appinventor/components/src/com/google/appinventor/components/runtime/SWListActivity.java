package com.google.appinventor.components.runtime;

import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.appinventor.components.runtime.util.AnimationUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity that presents the list of entities from the SemanticWebListPicker
 * @see LinkedDataListPicker
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class SWListActivity extends AppInventorCompatActivity {

  private static final String LOG_TAG = SWListActivity.class.getSimpleName();

  public abstract static class DataSource extends Binder {
    interface Completion {
      void onResultsAvailable(List<LabeledUri> results, boolean first);
      void done();
    }
    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) {
      final IBinder callback = data.readStrongBinder();
      performQuery(data.readString(), new Completion() {
        @Override
        public void onResultsAvailable(List<LabeledUri> results, boolean first) {
          Parcel parcel = Parcel.obtain();
          parcel.writeInt(first ? 1 : 0);
          parcel.writeInt(results.size());
          for (LabeledUri uri : results) {
            parcel.writeTypedObject(uri, 0);
          }
          try {
            callback.transact(FIRST_CALL_TRANSACTION, parcel, null, 0);
          } catch (RemoteException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void done() {
          Parcel parcel = Parcel.obtain();
          try {
            callback.transact(FIRST_CALL_TRANSACTION + 1, parcel, null, 0);
          } catch (RemoteException e) {
            e.printStackTrace();
          }
        }
      });
      return true;
    }

    abstract void performQuery(String query, Completion completion);
  }

  private class DataCallback extends Binder {
    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
      switch (code) {
        case FIRST_CALL_TRANSACTION:
          if (data.readInt() == 1) {
            items.clear();
          }
          int numItems = data.readInt();
          while (numItems > 0) {
            items.add(data.readTypedObject(LabeledUri.CREATOR));
            numItems--;
          }
          break;
        case FIRST_CALL_TRANSACTION + 1:
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Log.d(LOG_TAG, "Updating array adapter with " + items.size() + " items");
              listAdapter.clear();
              listAdapter.addAll(items);
              listView.invalidate();
            }
          });
        default:
          return super.onTransact(code, data, reply, flags);
      }
      return true;
    }
  }

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
    public String getUri() {
      return uri;
    }
    public String getLabel() {
      return label;
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
  private List<LabeledUri> originalItems;
  private List<LabeledUri> items;
  private IBinder source;
  private ArrayAdapter<LabeledUri> listAdapter;
  private ListView listView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LinearLayout viewLayout = new LinearLayout(this);
    viewLayout.setOrientation(LinearLayout.VERTICAL);
    EditText searchBox = new EditText(this);
    searchBox.setSingleLine();
    searchBox.setWidth(Component.LENGTH_FILL_PARENT);
    searchBox.setPadding(10, 10, 10, 10);
    searchBox.setHint("Search list...");
    if (!AppInventorCompatActivity.isClassicMode()) {
      searchBox.setBackgroundColor(Color.WHITE);
    }
    searchBox.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        listAdapter.getFilter().filter(s);
      }

      @Override
      public void afterTextChanged(Editable s) {

      }
    });
    listView = new ListView(this);
    viewLayout.addView(searchBox);
    viewLayout.addView(listView);
    listView.setTextFilterEnabled(true);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onListItemClick(SWListActivity.this.listView, view, position, id);
      }
    });
    setContentView(viewLayout);

    Intent myIntent = getIntent();
    if (myIntent.hasExtra(LinkedDataListPicker.SWLIST_ACTIVITY_ANIM_TYPE)) {
      closeAnim = myIntent.getStringExtra(LinkedDataListPicker.SWLIST_ACTIVITY_ANIM_TYPE);
    }
    if (myIntent.hasExtra(".source")) {
      Bundle callbackBundle = myIntent.getBundleExtra(".source");
      if (callbackBundle != null) {
        items = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);
        source = callbackBundle.getBinder("binder");
        if (source != null) {
          Parcel parcel = Parcel.obtain();
          parcel.writeStrongBinder(new DataCallback());
          parcel.writeString("");
          try {
            source.transact(IBinder.FIRST_CALL_TRANSACTION, parcel, null, 0);
          } catch (RemoteException e) {
            e.printStackTrace();
          }
        }
      }
    } else if (myIntent.hasExtra(LinkedDataListPicker.SWLIST_ACTIVITY_ARG_NAME)) {
      items = getIntent().getParcelableArrayListExtra(LinkedDataListPicker.SWLIST_ACTIVITY_ARG_NAME);
      originalItems = new ArrayList<>(items);
      listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
      listView.setAdapter(listAdapter);
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

  public void onListItemClick(ListView lv, View v, int position, long id) {
    Intent resultIntent = new Intent();
    LabeledUri selectedItem = (LabeledUri) listView.getItemAtPosition(position);
    resultIntent.putExtra(LinkedDataListPicker.SWLIST_ACTIVITY_RESULT_LABEL, selectedItem.label);
    resultIntent.putExtra(LinkedDataListPicker.SWLIST_ACTIVITY_RESULT_URI, selectedItem.uri);
    resultIntent.putExtra(LinkedDataListPicker.SWLIST_ACTIVITY_RESULT_INDEX, position + 1);
    closeAnim = selectedItem.label;
    setResult(RESULT_OK, resultIntent);
    finish();
    AnimationUtil.ApplyCloseScreenAnimation(this, closeAnim);
  }

}
