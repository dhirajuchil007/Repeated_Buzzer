package com.velocityappsdj.repeatingtimer


import com.google.common.truth.Truth.assertThat
import org.junit.Test


class TimeFormatUtilTest  {

    @Test
    fun `time less than 1 minute returns seconds`() {
        assertThat((TimeFormatUtil().getFormattedTime(24))).isEqualTo("00:00:24")
    }

    @Test
    fun `time more than 1 minute returns min and seconds`() {
        assertThat((TimeFormatUtil().getFormattedTime(125))).isEqualTo("00:02:05")
    }

    @Test
    fun `time more than 1 hour returns hours min adn seconds`() {
        assertThat((TimeFormatUtil().getFormattedTime(3665))).isEqualTo("01:01:05")
    }
}