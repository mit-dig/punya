// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.google.appinventor.components.runtime.util.ChartDataSourceUtil;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class to represent Chart Data Models. The class (and subclasses)
 * are used to handle the data part of the Chart component. One model
 * represents a single Data Series in a Chart (e.g. one line in a Line Chart
 * or one list of points in a Scatter Chart).
 * @param <T>  (Chart) DataSet type (MPAndroidChart DataSet class)
 * @param <D>  (Chart) Data type  (MPAndroidChart ChartData class)
 * @param <V>  (Chart) View that the model is compatible with (ChartView (sub)classes)
 */
public abstract class ChartDataModel<T extends DataSet, D extends ChartData, V extends ChartView> {
  protected D data;
  protected T dataset;
  protected V view;

  /**
   * Local List of entries; The modifications of the Data are made
   * directly to these Entries, which are meant to be detached from
   * the Dataset object itself to prevent exceptions & crashes due
   * to asynchronous operations
  */
  protected List<Entry> entries;

  /**
   * Limit the maximum allowed real-time data entries
   * Since real-time data comes in fast, the case of
   * multi-data source input is unhandled since it's
   * better to avoid it.
  */
  protected int maximumTimeEntries = 200;

  /**
   * Enum used to specify the criterion to use for entry filtering/comparing.
   */
  public enum EntryCriterion {
    All, // Return all entries
    XValue,
    YValue;
  }

  /**
   * Initializes a new ChartDataModel object instance.
   *
   * @param data Chart data instance
   * @param view Chart View to link model to
   */
  protected ChartDataModel(D data, V view) {
    this.data = data;
    this.view = view;

    entries = new ArrayList<Entry>();
  }

  /**
   * Returns the size of the tuples that this Data Series
   * accepts.
   *
   * @return tuple size (integer)
   */
  protected abstract int getTupleSize();

  /**
   * Returns the Data Series of the Data Model.
   * The method is made synchronized to avoid concurrent
   * access between threads, which can cause exceptions
   * if multiple threads try to modify the Dataset object
   * at the same time.
   *
   * @return Data Series object of the Data model
   */
  public T getDataset() {
    return dataset;
  }

  public ChartData getData() {
    return data;
  }

  /**
   * Changes the color of the data set.
   *
   * @param argb new color
   */
  public void setColor(int argb) {
    getDataset().setColor(argb);
  }

  /**
   * Changes the colors of the Data Series from the passed in Colors List.
   *
   * @param colors List of colors to set to the Data Series
   */
  public void setColors(List<Integer> colors) {
    // With regards to the Colors property setting for the
    // ScatterChartDataModel, currently an issue exists:
    // https://github.com/PhilJay/MPAndroidChart/issues/4483
    // which sets the same color to 2 points at once.
    getDataset().setColors(colors);
  }

  /**
   * Changes the label of the data set.
   *
   * @param text new label text
   */
  public void setLabel(String text) {
    getDataset().setLabel(text);
  }

  /**
   * Sets the elements of the Data Series from a CSV-formatted String.
   *
   * @param elements String in CSV format
   */
  public void setElements(String elements) {
    // Get the expected number of tuples
    int tupleSize = getTupleSize();

    // Split all the CSV entries by comma
    String[] entries = elements.split(",");

    // Iterate over every tuple (by grouping entries)
    // We start from tupleSize - 1 since the (tupleSize - 1)-th
    // entry will be the last entry of the tuple.
    // The index is incremented by the tupleSize to move to the next
    // group of entries for a tuple.
    for (int i = tupleSize - 1; i < entries.length; i += tupleSize) {
      List<String> tupleEntries = new ArrayList<String>();

      // Iterate over all the tuple entries
      // First entry is in (i - tupleSize + 1)
      for (int j = tupleSize - 1; j >= 0; --j) {
        int index = i - j;
        tupleEntries.add(entries[index]);
      }

      // Add entry from the parsed tuple
      addEntryFromTuple(YailList.makeList(tupleEntries));
    }
  }

  /**
   * Imports data from a List object.
   * Valid tuple entries are imported, and the invalid entries are ignored.
   *
   * @param list List containing tuples
   */
  public void importFromList(List list) {
    // Iterate over all the entries of the List
    for (Object entry : list) {
      YailList tuple = null;

      if (entry instanceof YailList) {
        // Convert entry to YailList
        tuple = (YailList) entry;
      } else if (entry instanceof List) {
        // List has to be converted to a YailList
        tuple = YailList.makeList((List) entry);
      }

      // Entry could be parsed to a YailList; Attempt importing from
      // the constructed tuple.
      if (tuple != null) {
        addEntryFromTuple(tuple);
      }
    }
  }

  /**
   * Removes the specified List of values, which are expected to be tuples.
   * Invalid entries are ignored.
   *
   * @param values List of values to remove
   */
  public void removeValues(List values) {
    // Iterate all the entries of the generic List)
    for (Object entry : values) {
      YailList tuple = null;

      // Entry is a List; Possibly a tuple
      if (entry instanceof YailList) {
        tuple = (YailList) entry;
      } else if (entry instanceof List) {
        // Create a tuple from the entry
        tuple = YailList.makeList((List) entry);
      }

      // Attempt to remove entry
      removeEntryFromTuple(tuple);
    }
  }

  /**
   * Imports data from the specified list of columns.
   * Tuples are formed from the rows of the combined
   * columns in order of the columns.
   * <p>
   * The first element is skipped, since it is assumed that it
   * is the column name.
   *
   * @param columns columns to import data from
   */
  public void importFromColumns(YailList columns) {
    // Get a YailList of tuples from the specified columns
    YailList tuples = getTuplesFromColumns(columns);

    // Use the generated tuple list in the importFromList method to
    // import the data.
    importFromList(tuples);
  }

  /**
   * Constructs and returns a List of tuples from the specified Columns List.
   * The Columns List is expected to be a List containing Lists, where each
   * List corresponds to a column, the first entry of which is the header/name
   * of the column (hence it is skipped in generating data)
   *
   * @param columns List of columns to generate tuples from
   * @return Generated List of tuples from the columns
   */
  public YailList getTuplesFromColumns(YailList columns) {
    // Determine the (maximum) row count of the specified columns
    int rows = ChartDataSourceUtil.determineMaximumListSize(columns);

    List<YailList> tuples = new ArrayList<YailList>();

    // Generate tuples from the columns
    for (int i = 1; i < rows; ++i) {
      ArrayList<String> tupleElements = new ArrayList<String>();

      // Add entries to the tuple from all i-th values (i-th row)
      // of the data columns.
      for (int j = 0; j < columns.size(); ++j) {
        Object value = columns.getObject(j);

        // Invalid column specified; Add default value (minus one to
        // compensate for the skipped value)
        if (!(value instanceof YailList)) {
          tupleElements.add(getDefaultValue(i - 1));
          continue;
        }

        // Safe-cast value to YailList
        YailList column = (YailList) value;

        if (column.size() > i) { // Entry exists in column
          // Add entry from column
          tupleElements.add(column.getString(i));
        } else if (column.size() == 0) { // Column empty (default value should be used)
          // Use default value instead (we use an index minus one to componsate
          // for the skipped initial value)
          tupleElements.add(getDefaultValue(i - 1));
        } else { // Column too small
          // Add blank entry (""), up for the addEntryFromTuple method
          // to interpret.
          tupleElements.add("");

          // TODO: Make this a user-configurable flag
          // Use default value instead
          //tupleElements.add(getDefaultValue(i));
        }
      }

      // Create the YailList tuple representation and add it to the
      // list of tuples used.
      YailList tuple = YailList.makeList(tupleElements);
      tuples.add(tuple);
    }

    // Return result as YailList
    return YailList.makeList(tuples);
  }

  /**
   * Adds an entry from a specified tuple.
   *
   * @param tuple Tuple representing the entry to add
   */
  public abstract void addEntryFromTuple(YailList tuple);

  /**
   * Removes an entry from the Data Series from the specified
   * tuple (provided the entry exists)
   *
   * @param tuple Tuple representing the entry to remove
   */
  public void removeEntryFromTuple(YailList tuple) {
    // Construct an entry from the specified tuple
    Entry entry = getEntryFromTuple(tuple);

    if (entry != null) {
      // TODO: The commented line should be used instead. However, the library does not (yet) implement
      // TODO: equals methods in it's entries as of yet, so the below method fails.
      // dataset.removeEntry(entry);

      // Get the index of the entry
      int index = findEntryIndex(entry);

      removeEntry(index);
    }
  }

  /**
   * Removes the entry in the specified index, provided that the
   * index is within bounds.
   *
   * @param index Index of the Entry to remove
   */
  public void removeEntry(int index) {
    // Entry exists; remove it
    if (index >= 0) {
      entries.remove(index);
    }
  }

  /**
   * Checks whether an entry exists in the Data Series.
   *
   * @param tuple Tuple representing the entry to look for
   * @return true if the Entry exists, false otherwise
   */
  public boolean doesEntryExist(YailList tuple) {
    // Construct the entry from the specified tuple
    Entry entry = getEntryFromTuple(tuple);

    // Get the index of the entry
    int index = findEntryIndex(entry);

    // Entry exists only if index is non-negative
    return index >= 0;
  }

  /**
   * Finds and returns all the entries by the specified criterion and value.
   * <p>
   * The entries are returned as tuple (YailList) representations.
   *
   * @param value     value to use for comparison
   * @param criterion criterion to use for comparison
   * @return YailList of entries represented as tuples matching the specified conditions
   */
  public YailList findEntriesByCriterion(String value, EntryCriterion criterion) {
    List<YailList> entries = new ArrayList<YailList>();

    for (Entry entry : this.entries) {
      // Check whether the provided criterion & value combination are satisfied
      // according to the current Entry
      if (isEntryCriterionSatisfied(entry, criterion, value)) {
        // Criterion satisfied; Add entry to resulting List
        entries.add(getTupleFromEntry(entry));
      }
    }

    return YailList.makeList(entries);
  }

  /**
   * Returns all the entries of the Data Series in the form of tuples (YailLists)
   *
   * @return YailList of all entries represented as tuples
   */
  public YailList getEntriesAsTuples() {
    // Use the All criterion to get all the Entries
    return findEntriesByCriterion("0", EntryCriterion.All);
  }

  /**
   * Check whether the entry matches the specified criterion.
   *
   * @param entry     entry to check against
   * @param criterion criterion to check with (e.g. x value)
   * @param value     value to use for comparison (as a String)
   * @return true if the entry matches the criterion
   */
  protected boolean isEntryCriterionSatisfied(Entry entry, EntryCriterion criterion, String value) {
    boolean criterionSatisfied = false;

    switch (criterion) {
      case All: // Criterion satisfied no matter the value, since all entries should be returned
        criterionSatisfied = true;
        break;

      case XValue: // Criterion satisfied based on x value match with the value
        // PieEntries and regular entries require different
        // handling sine PieEntries have String x values
        if (entry instanceof PieEntry) {
          // Criterion is satisfied for a Pie Entry only if
          // the label is equal to the specified value
          PieEntry pieEntry = (PieEntry) entry;
          criterionSatisfied = pieEntry.getLabel().equals(value);
        } else {
          // X value is a float, so it has to be parsed and
          // compared. If parsing fails, the criterion is
          // not satisfied.
          try {
            float xValue = Float.parseFloat(value);
            float compareValue = entry.getX();

            // Since Bar Chart grouping applies offsets to x values,
            // and the x values are expected to be integers, the
            // value has to be floored.
            if (entry instanceof BarEntry) {
              compareValue = (float) Math.floor(compareValue);
            }

            criterionSatisfied = (compareValue == xValue);
          } catch (NumberFormatException e) {
            // Do nothing (value already false)
          }
        }
        break;

      case YValue: // Criterion satisfied based on y value match with the value
        try {
          // Y value is always a float, therefore the String value has to
          // be parsed.
          float yValue = Float.parseFloat(value);
          criterionSatisfied = (entry.getY() == yValue);
        } catch (NumberFormatException e) {
          // Do nothing (value already false)
        }
        break;
    }

    return criterionSatisfied;
  }

  /**
   * Creates an Entry from the specified tuple.
   *
   * @param tuple Tuple representing the entry to create
   * @return new Entry object instance representing the specified tuple
   */
  public abstract Entry getEntryFromTuple(YailList tuple);

  /**
   * Returns a YailList tuple representation of the specfied entry
   *
   * @param entry Entry to convert to tuple
   * @return tuple (YailList) representation of the Entry
   */
  public abstract YailList getTupleFromEntry(Entry entry);

  /**
   * Finds the index of the specified Entry in the Data Series.
   * Returns -1 if the Entry does not exist.
   * <p>
   * TODO: Primarily used due to equals not implemented in MPAndroidChart (needed for specific operations)
   * TODO: In the future, this method will probably become obsolete if it ever gets fixed (post-v3.1.0).
   *
   * @param entry Entry to find
   * @return index of the entry, or -1 if entry is not found
   */
  protected int findEntryIndex(Entry entry) {
    for (int i = 0; i < entries.size(); ++i) {
      Entry currentEntry = entries.get(i);

      // Check whether the current entry is equal to the
      // specified entry. Note that (in v3.1.0), equals()
      // does not yield the same result.
      if (areEntriesEqual(currentEntry, entry)) {
        // Entry matched; Return
        return i;
      }
    }

    return -1;
  }

  /**
   * Deletes all the entries in the Data Series.
   */
  public void clearEntries() {
    entries.clear();
  }

  /**
   * Adds the specified entry as a time entry to the Data Series.
   * <p>
   * The method handles additional logic for removing excess values
   * if the count exceeds the threshold.
   *
   * @param tuple tuple representing the time entry
   */
  public void addTimeEntry(YailList tuple) {
    // If the entry count of the Data Series entries exceeds
    // the maximum allowed time entries, then remove the first one
    if (entries.size() >= maximumTimeEntries) {
      entries.remove(0);
    }

    // Add entry from the specified tuple
    // TODO: Support for multi-dimensional case (currently tuples always consist
    // TODO: of two elements)
    addEntryFromTuple(tuple);
  }

  /**
   * Sets the maximum time entries to be kept in the Data Series
   *
   * @param entries number of entries to keep
   */
  public void setMaximumTimeEntries(int entries) {
    maximumTimeEntries = entries;
  }

  /**
   * Sets the default styling properties of the Data Series.
   */
  protected void setDefaultStylingProperties() {
    /*
        The method body is left empty to not require data models
        which do not need any default styling properties to override
        the method by default.
     */
  }

  /**
   * Returns default tuple entry value to use when a value
   * is not present.
   *
   * @param index index for the value
   * @return value corresponding to the specified index
   */
  protected String getDefaultValue(int index) {
    // Return value which directly corresponds to the index
    // number. So default values go as 0, 1, 2, ..., N
    return index + "";
  }

  /**
   * Checks equality between two entries.
   * <p>
   * TODO: REMARK
   * TODO: The reason why this method is needed is due to the equals()
   * TODO: and equalTo() methods not being implemented fully to fit
   * TODO: the requirements of the comparison done in the models.
   * TODO: equalTo() does not check label equality (for Pie Charts)
   * TODO: and equals() checks memory references instead of values.
   *
   * @param e1 first Entry to compare
   * @param e2 second Entry to compare
   * @return true if the entries are equal
   */
  protected boolean areEntriesEqual(Entry e1, Entry e2) {
    return e1.equalTo(e2);
  }

  /**
   * Returns the entries of the Chart Data Model.
   *
   * @return List of entries of the Chart Data Model (Data Series)
   */
  public List<Entry> getEntries() {
    return Collections.unmodifiableList(entries);
  }
}
