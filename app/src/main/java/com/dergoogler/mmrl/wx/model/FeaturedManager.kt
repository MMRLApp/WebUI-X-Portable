package com.dergoogler.mmrl.wx.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dergoogler.mmrl.datastore.model.WorkingMode
import com.dergoogler.mmrl.ui.component.dialog.RadioOptionItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.RadioDialogItem
import com.dergoogler.mmrl.wx.R

data class FeaturedManager(
    @StringRes val name: Int,
    @DrawableRes val icon: Int,
    val workingMode: WorkingMode,
) {
    @Composable
    fun toRadioOption() = RadioOptionItem(
        title = stringResource(name),
        value = workingMode
    )

    @Composable
    fun toRadioDialogItem() = RadioDialogItem(
        title = stringResource(name),
        value = workingMode
    )

    companion object {
        val managers
            get() = listOf(
                FeaturedManager(
                    name = R.string.magisk,
                    icon = com.dergoogler.mmrl.ui.R.drawable.magisk_logo,
                    workingMode = WorkingMode.MODE_MAGISK,
                ),

                FeaturedManager(
                    name = R.string.kernelsu,
                    icon = com.dergoogler.mmrl.ui.R.drawable.kernelsu_logo,
                    workingMode = WorkingMode.MODE_KERNEL_SU,
                ),

                FeaturedManager(
                    name = R.string.kernelsu_next,
                    icon = com.dergoogler.mmrl.ui.R.drawable.kernelsu_next_logo,
                    workingMode = WorkingMode.MODE_KERNEL_SU_NEXT,
                ),

                FeaturedManager(
                    name = R.string.apatch,
                    icon = com.dergoogler.mmrl.ui.R.drawable.brand_android,
                    workingMode = WorkingMode.MODE_APATCH
                ),

                FeaturedManager(
                    name = R.string.sukisu,
                    icon = com.dergoogler.mmrl.ui.R.drawable.sukisu_logo,
                    workingMode = WorkingMode.MODE_SUKISU
                ),

                FeaturedManager(
                    name = R.string.non_root,
                    icon = R.drawable.shield_lock,
                    workingMode = WorkingMode.MODE_NON_ROOT
                ),
            )
    }
}