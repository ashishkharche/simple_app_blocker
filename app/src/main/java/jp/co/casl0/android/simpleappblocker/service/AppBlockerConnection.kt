/*
 * Copyright 2022 CASL0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.casl0.android.simpleappblocker.service

import android.os.ParcelFileDescriptor
import com.orhanobut.logger.Logger
import jp.co.casl0.android.simpleappblocker.PcapPlusPlusInterface
import jp.co.casl0.android.simpleappblocker.model.PacketInfo
import jp.co.casl0.android.simpleappblocker.utils.getNowDateTime
import kotlinx.coroutines.Runnable
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

class AppBlockerConnection(private val tunnelInterface: ParcelFileDescriptor) : Runnable {
    companion object {
        private const val MAX_PACKET_SIZE = Short.MAX_VALUE.toInt()
    }

    interface OnBlockPacketListener {
        fun onBlockPacket(packetInfo: PacketInfo)
    }

    private var _listener: OnBlockPacketListener? = null

    fun setOnBlockPacketListener(onBlockPacketListener: OnBlockPacketListener) {
        _listener = onBlockPacketListener
    }

    override fun run() {
        val fis = FileInputStream(tunnelInterface.fileDescriptor)
        val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)
        while (true) {
            val length: Int = try {
                fis.read(packet.array())
            } catch (e: Exception) {
                when (e) {
                    is ReadOnlyBufferException, is UnsupportedOperationException -> {
                        Logger.d("ByteBuffer.array failed")
                    }
                    is IOException -> {
                        Logger.d("FileInputStream.read failed")
                    }
                    else -> {
                        Logger.d("unexpected failure")
                    }
                }
                e.localizedMessage?.let { Logger.d(it) }
                0
            }
            if (length > 0) {
                _listener?.onBlockPacket(
                    PacketInfo(
                        PcapPlusPlusInterface.getSrcIpAddressNative(packet.array(), length),
                        PcapPlusPlusInterface.getSrcPortNative(packet.array(), length),
                        PcapPlusPlusInterface.getDstIpAddressNative(packet.array(), length),
                        PcapPlusPlusInterface.getDstPortNative(packet.array(), length),
                        PcapPlusPlusInterface.getProtocolAsStringNative(packet.array(), length),
                        getNowDateTime()
                    )
                )
            }
            packet.clear()
            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                Logger.d("AppBlockerConnection interrupted")
                tunnelInterface.close()
                return
            }
        }
    }
}
