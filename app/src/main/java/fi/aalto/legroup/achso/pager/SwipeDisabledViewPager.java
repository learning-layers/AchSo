/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.pager;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
* Created by purma on 6.5.2014.
*/
public class SwipeDisabledViewPager extends ViewPager {
    private boolean mSwipeEnabled;

    public SwipeDisabledViewPager(Context context) {
        super(context);
    }

    public SwipeDisabledViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void setSwipeEnabled(boolean b) {
        mSwipeEnabled = b;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mSwipeEnabled) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mSwipeEnabled) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    public boolean getSwipeEnabled() {
        return mSwipeEnabled;
    }
}
