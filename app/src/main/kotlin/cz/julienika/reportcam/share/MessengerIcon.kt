package cz.julienika.reportcam.share

import cz.julienika.reportcam.R

/** The brand icon the Owner supplied for each messenger (Boxicons brands, MIT; see README). */
val Messenger.icon: Int
    get() =
        when (this) {
            Messenger.WHATSAPP -> R.drawable.ic_msg_whatsapp
            Messenger.TELEGRAM -> R.drawable.ic_msg_telegram
            Messenger.MESSENGER -> R.drawable.ic_msg_messenger
            Messenger.DISCORD -> R.drawable.ic_msg_discord
            Messenger.SLACK -> R.drawable.ic_msg_slack
            Messenger.SNAPCHAT -> R.drawable.ic_msg_snapchat
            Messenger.INSTAGRAM -> R.drawable.ic_msg_instagram
        }
