package com.example.pugprint.ui.home

import androidx.annotation.StringRes
import com.example.pugprint.R
import com.example.pugprint.design.components.BannerKind

/** How a [HomeMessage] is shown: a banner with one line and, for problems, what to do about it. */
data class MessageBanner(
    val kind: BannerKind,
    @param:StringRes val text: Int,
    @param:StringRes val hint: Int? = null,
)

/** Every message the app can show the kid, in her words: what happened and what to do next. */
fun HomeMessage.banner(): MessageBanner =
    when (this) {
        HomeMessage.PrintDone ->
            MessageBanner(BannerKind.Success, R.string.message_print_done, R.string.message_print_done_hint)
        HomeMessage.PrintNoPaper ->
            MessageBanner(BannerKind.Problem, R.string.message_print_no_paper, R.string.message_print_no_paper_hint)
        HomeMessage.PrintPaperOrLid ->
            MessageBanner(
                BannerKind.Problem,
                R.string.message_print_paper_or_lid,
                R.string.message_print_paper_or_lid_hint,
            )
        HomeMessage.PrintDisconnected ->
            MessageBanner(
                BannerKind.Problem,
                R.string.message_print_disconnected,
                R.string.message_print_disconnected_hint,
            )
        HomeMessage.PrintFailed ->
            MessageBanner(BannerKind.Problem, R.string.message_print_failed, R.string.message_print_failed_hint)
        HomeMessage.PairingCancelled ->
            MessageBanner(BannerKind.Info, R.string.message_pairing_cancelled, R.string.message_pairing_cancelled_hint)
        HomeMessage.PairingUnavailable ->
            MessageBanner(
                BannerKind.Problem,
                R.string.message_pairing_unavailable,
                R.string.message_pairing_unavailable_hint,
            )
        HomeMessage.PermissionDenied ->
            MessageBanner(
                BannerKind.Problem,
                R.string.message_permission_denied,
                R.string.message_permission_denied_hint,
            )
    }
