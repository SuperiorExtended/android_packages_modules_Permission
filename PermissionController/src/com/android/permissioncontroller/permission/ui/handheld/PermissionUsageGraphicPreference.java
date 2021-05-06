/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.permissioncontroller.permission.ui.handheld;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.permissioncontroller.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A Preference for the permission usage graphic.
 */
public class PermissionUsageGraphicPreference extends Preference {

    /** Permission group to count mapping. */
    private @NonNull Map<String, Integer> mUsages = new HashMap<>();

    /** Whether to show the "Other" category. */
    private boolean mShowOtherCategory;

    public PermissionUsageGraphicPreference(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public PermissionUsageGraphicPreference(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PermissionUsageGraphicPreference(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PermissionUsageGraphicPreference(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.permission_usage_graphic);
        setSelectable(false);
    }

    /** Sets permission group usages: map of group name to usage count. */
    public void setUsages(Map<String, Integer> usages) {
        if (!Objects.equals(mUsages, usages)) {
            mUsages = usages;
            notifyChanged();
        }
    }

    /** Sets whether to show the "Other" category. */
    public void setShowOtherCategory(boolean showOtherCategory) {
        if (mShowOtherCategory != showOtherCategory) {
            mShowOtherCategory = showOtherCategory;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        boolean isUsagesEmpty = isUsagesEmpty();

        CompositeCircleView ccv =
                (CompositeCircleView) holder.findViewById(R.id.composite_circle_view);
        CompositeCircleViewLabeler ccvl = (CompositeCircleViewLabeler) holder.findViewById(
                R.id.composite_circle_view_labeler);

        // Set center text.
        // TODO(b/176902658): Fix text appearance.
        TextView centerLabel = new TextView(getContext());
        centerLabel.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        centerLabel.setText(getContext().getString(R.string.privdash_label_24h));
        centerLabel.setTextAppearance(R.style.PrivacyDashboardGraphicLabel);

        // Create labels, counts, and colors.
        TextView[] labels;
        int[] counts;
        int[] colors;
        if (isUsagesEmpty) {
            // Special case if usages are empty.
            labels = new TextView[] { new TextView(getContext()) };
            labels[0].setText(getContext().getString(R.string.privdash_label_none));
            counts = new int[] { 1 };
            // TODO(b/176902658): Determine base color, and best way to blend below.
            colors = new int[] { Color.GRAY };
        } else {
            labels = new TextView[] {
                    new TextView(getContext()),
                    new TextView(getContext()),
                    new TextView(getContext()),
                    new TextView(getContext())
            };
            labels[0].setText(getContext().getString(R.string.privdash_label_camera));
            labels[1].setText(getContext().getString(R.string.privdash_label_microphone));
            labels[2].setText(getContext().getString(R.string.privdash_label_location));
            labels[3].setText(getContext().getString(R.string.privdash_label_other));
            counts = new int[] {
                    getUsageCount(Manifest.permission_group.CAMERA),
                    getUsageCount(Manifest.permission_group.MICROPHONE),
                    getUsageCount(Manifest.permission_group.LOCATION),
                    mShowOtherCategory
                            ? getUsageCountExcluding(Manifest.permission_group.CAMERA,
                            Manifest.permission_group.MICROPHONE,
                            Manifest.permission_group.LOCATION) : 0
            };
            // TODO(b/176902658): Determine colors from system theme / sampled color.
            // These are placeholders.
            colors = new int[] {
                    0xff4285f4,
                    0xffea4335,
                    0xfffbbc05,
                    0xff34a853
            };
        }

        // Set label styles.
        for (int i = 0; i < labels.length; i++) {
            labels[i].setTextAppearance(R.style.PrivacyDashboardGraphicLabel);
        }

        // Get circle-related dimensions.
        TypedValue outValue = new TypedValue();
        getContext().getResources().getValue(R.dimen.privhub_label_radius_scalar,
                outValue, true);
        float labelRadiusScalar = outValue.getFloat();
        int circleStrokeWidth = (int) getContext().getResources().getDimension(
                R.dimen.privhub_circle_stroke_width);

        // Configure circle and labeler.
        ccvl.configure(R.id.composite_circle_view, centerLabel, labels, labelRadiusScalar);
        // Start at angle 300 (top right) to allow for small segments for cam, mic, and loc.
        ccv.configure(300, counts, colors, circleStrokeWidth);
    }

    private int getUsageCount(String group) {
        Integer count = mUsages.get(group);
        if (count == null) {
            return 0;
        }
        return count;
    }

    private int getUsageCountExcluding(String... excludeGroups) {
        int count = 0;
        List<String> exclude = Arrays.asList(excludeGroups);
        for (Map.Entry<String, Integer> entry : mUsages.entrySet()) {
            if (exclude.indexOf(entry.getKey()) >= 0) {
                continue;
            }
            count += entry.getValue();
        }
        return count;
    }

    private boolean isUsagesEmpty() {
        return getUsageCountExcluding() == 0;
    }
}