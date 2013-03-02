/*
 * Copyright 2013 Younghoon Park
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.yhpark.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class FloatingLayout extends ViewGroup {
	private int gravity = Gravity.LEFT | Gravity.TOP;

	private int floorCount;
	private int childrenHeightSum;

	public FloatingLayout(Context context) {
		super(context);
	}

	public FloatingLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray style = context.obtainStyledAttributes(attrs, R.styleable.FloatingLayout);

		gravity = style.getInt(R.styleable.FloatingLayout_android_gravity, gravity);

		style.recycle();
	}
	
	public FloatingLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray style = context.obtainStyledAttributes(attrs, R.styleable.FloatingLayout, defStyle, 0);

		gravity = style.getInt(R.styleable.FloatingLayout_android_gravity, gravity);

		style.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int contentWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
		final int contentHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();

		int widthUsed = 0, heightUsed = 0;
		int floorHeight = 0;

		int floorCount = 1;
		int maxWidthUsed = 0;
		
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();

			// child measure
			measureChildWithMargins(child, 
					MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.AT_MOST), 
					0, // widthUsed
					MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.AT_MOST), 
					heightUsed);

			// floor++
			if ((child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin) > contentWidth - widthUsed) {
				widthUsed = 0;
				heightUsed += floorHeight;
				floorHeight = 0;
				
				floorCount++;

				measureChildWithMargins(child, 
						MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.AT_MOST), 
						0, // widthUsed
						MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.AT_MOST), 
						heightUsed);
			}

			widthUsed += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
			maxWidthUsed = Math.max(widthUsed, maxWidthUsed);
			floorHeight = Math.max(child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin, floorHeight);
		}

		heightUsed += floorHeight;
		
		// values for onLayout()
		this.floorCount = floorCount;
		this.childrenHeightSum = heightUsed;

		// suggested minimum width/height
		int measuredWidth = Math.max(getSuggestedMinimumWidth(), maxWidthUsed + getPaddingLeft() + getPaddingRight());
		int measuredHeight = Math.max(getSuggestedMinimumHeight(), heightUsed + getPaddingTop() + getPaddingBottom());

		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY)
			measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)
			measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int widthUsed = 0, heightUsed = 0;
		int floorHeight = 0;
		
		switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
		case Gravity.TOP:
		case Gravity.FILL_VERTICAL:
			// heightUsed is 0
			break;
		case Gravity.CENTER_VERTICAL:
			heightUsed = (getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - childrenHeightSum) / 2;
			break;
		case Gravity.BOTTOM:
			heightUsed = getMeasuredHeight() - getPaddingBottom() - childrenHeightSum;
			break;
		}
		
		// for FILL_VERTICAL
		int verticalEmptySplit = 0;
		if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL && floorCount > 1)
			verticalEmptySplit = (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - childrenHeightSum) / (floorCount - 1);
		
		int floorStartIdx = 0;
		
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();

			int left = getPaddingLeft() + widthUsed + lp.leftMargin;

			if (left + child.getMeasuredWidth() + lp.rightMargin + getPaddingRight() > getMeasuredWidth()) {
				layoutFloor(floorStartIdx, i, heightUsed);
				
				widthUsed = 0;
				heightUsed += floorHeight + verticalEmptySplit;
				floorHeight = 0;
				
				floorStartIdx = i;
				
				left = getPaddingLeft() + widthUsed + lp.leftMargin;
			}

			widthUsed += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
			floorHeight = Math.max(child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin, floorHeight);
		}
		
		layoutFloor(floorStartIdx, getChildCount(), heightUsed);
	}
	
	private void layoutFloor(int startInclusive, int endExclusive, int floorTop) {
		int floorContentWidthSum = 0;
		int floorHeight = 0;
		
		// calculate floorContentWidthSum
		
		for (int i = startInclusive; i < endExclusive; i++) {
			View child = getChildAt(i);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
			
			floorContentWidthSum += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
			floorHeight = Math.max(child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin, floorHeight);
		}
		
		int contentLeft = 0;
		
		// contentLeft by alignMode
		switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
		case Gravity.LEFT:
		case Gravity.FILL_HORIZONTAL:
			contentLeft = getPaddingLeft();
			break;
		case Gravity.CENTER_HORIZONTAL:
			contentLeft = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - floorContentWidthSum) / 2;
			break;
		case Gravity.RIGHT:
			contentLeft = getMeasuredWidth() - getPaddingRight() - floorContentWidthSum;
			break;
		default:
			contentLeft = getPaddingLeft();
		}
		
		int widthUsed = 0;
		
		// for FILL_HORIZONTAL
		int emptySplit = 0;
		if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL && (endExclusive - startInclusive) > 1)
			emptySplit = (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - floorContentWidthSum) 
				/ (endExclusive - startInclusive - 1);
		
		for (int i = startInclusive; i < endExclusive; i++) {
			View child = getChildAt(i);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
			
			int left = contentLeft + widthUsed + lp.leftMargin;
			int top = getPaddingTop() + floorTop + lp.topMargin;
			
			
			switch (lp.gravity & Gravity.VERTICAL_GRAVITY_MASK) {
			case Gravity.TOP:
				top = getPaddingTop() + floorTop + lp.topMargin;
				break;
			case Gravity.CENTER_VERTICAL:
				top = getPaddingTop() + floorTop + (floorHeight - child.getMeasuredHeight() - lp.topMargin - lp.bottomMargin) / 2 + lp.topMargin;
				break;
			case Gravity.BOTTOM:
				top = getMeasuredHeight() - getPaddingBottom() - child.getMeasuredHeight() - lp.bottomMargin;
				break;
			}
			
			child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
			
			widthUsed += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin + emptySplit;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new FrameLayout.LayoutParams(getContext(), attrs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof FrameLayout.LayoutParams;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected FrameLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new FrameLayout.LayoutParams(p);
	}
}
