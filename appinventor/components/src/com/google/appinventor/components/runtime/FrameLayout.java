// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

/* 
 * @author fuming@mit.mit (Fuming Shih)
 * @author wli17@mit.edu (Weihua Li)
 */
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams;


public class FrameLayout implements Layout {
  private final android.widget.FrameLayout layoutManager;

  FrameLayout(Context context) {

    layoutManager = new android.widget.FrameLayout(context);

  }
  @Override
  public ViewGroup getLayoutManager() {
    // TODO Auto-generated method stub
    Log.i("FrameLayout", "some one just get my framelayout");

    return layoutManager;
  }

  @Override
  public void add(AndroidViewComponent component) {
    // TODO Auto-generated method stub
    Log.i("FrameLayout", "adding component..");
    layoutManager.addView(component.getView(), new LayoutParams(
        LayoutParams.FILL_PARENT,  // width
        LayoutParams.WRAP_CONTENT));
  }


  public List<Object> getChildren(){

     ArrayList<Object> children = new ArrayList<Object>();
    int childcount = layoutManager.getChildCount();
    for (int i=0; i < childcount; i++){
      View v = layoutManager.getChildAt(i);
      children.add(v);
    }
     return children;
  }

}
