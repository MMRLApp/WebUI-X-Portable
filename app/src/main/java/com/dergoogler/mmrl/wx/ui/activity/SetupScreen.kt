package com.dergoogler.mmrl.wx.ui.activity

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dergoogler.mmrl.datastore.model.WorkingMode
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.ui.component.card.Card
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.RadioItem
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Icon
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.model.FeaturedManager
import com.dergoogler.mmrl.wx.util.toWorkingMode

@Composable
fun SetupScreen(setWorkingMode: (WorkingMode) -> Unit) {
    var currentSelection: FeaturedManager? by remember { mutableStateOf(null) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.welcome),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.select_your_platform),
                        fontSize = 20.sp,
                        modifier = Modifier.alpha(.3f)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = FeaturedManager.managers,
                        key = { it.workingMode.name }
                    ) { manager ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val selected =
                            remember(currentSelection) { currentSelection == manager }

                        Card {
                            List(
                                modifier = Modifier.relative()
                            ) {
                                RadioItem(
                                    selected = selected,
                                    interactionSource = interactionSource,
                                    onClick = {
                                        currentSelection = manager
                                    }
                                ) {
                                    Icon(painter = painterResource(manager.icon))
                                    Title(manager.name)
                                }
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        enabled = currentSelection != null,
                        onClick = {
                            setWorkingMode(currentSelection!!.workingMode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .absolutePadding(bottom = 5.dp),
                    ) {
                        Text(
                            text = currentSelection.nullable(default = {
                                stringResource(R.string.select)
                            }) {
                                stringResource(
                                    R.string.continue_with,
                                    stringResource(currentSelection!!.name)
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.non_root_note),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(.3f)
                    )
                }
            }
        }
    }
}
