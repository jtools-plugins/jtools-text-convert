package com.lhstack.text

import cn.hutool.core.text.csv.CsvUtil
import cn.hutool.poi.excel.ExcelUtil
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBSplitter
import com.lhstack.text.component.MultiLanguageTextField
import com.lhstack.tools.plugins.Helper
import com.lhstack.tools.plugins.IPlugin
import org.apache.commons.lang.StringEscapeUtils
import java.awt.BorderLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.swing.*
import kotlin.streams.toList


class PluginImpl : IPlugin {

    companion object {
        val CACHE = mutableMapOf<String, JComponent>()
        val DISPOSERS = mutableMapOf<String, Disposable>()
    }

    override fun pluginIcon(): Icon = Helper.findIcon("pluginIcon.svg", PluginImpl::class.java)

    override fun pluginTabIcon(): Icon = Helper.findIcon("pluginTabIcon.svg", PluginImpl::class.java)

    override fun closeProject(project: Project) {
        DISPOSERS.remove(project.locationHash)?.let {
            Disposer.dispose(it)
        }
    }

    override fun createPanel(project: Project): JComponent {
        return CACHE.computeIfAbsent(project.locationHash) {
            val disposable = Disposer.newDisposable()
            DISPOSERS[project.locationHash] = disposable
            SimpleToolWindowPanel(false).apply {
                this.setContent(JBSplitter(true).let { splitter ->
                    val input = MultiLanguageTextField(PlainTextFileType.INSTANCE, project, "", true)
                    val output = MultiLanguageTextField(PlainTextFileType.INSTANCE, project, "", true)
                    Disposer.register(disposable, input)
                    Disposer.register(disposable, output)
                    val splitterTextField = JTextField("\\n").apply { this.toolTipText = "默认\\n作为分隔符" }
                    val mapTextField =
                        JTextField("'\${value}'").apply { this.toolTipText = "\${value}作为默认填充变量" }
                    val joinTextField = JTextField(",").apply { this.toolTipText = ",作为默认连接符号" }
                    splitter.firstComponent = JPanel(BorderLayout()).apply {
                        this.add(JPanel(BorderLayout()).apply {
                            this.add(JButton("导入").apply {
                                this.addActionListener {
                                    val fileDescriptor =
                                        FileChooserDescriptorFactory.createSingleFileDescriptor()
                                    fileDescriptor.withFileFilter {
                                        when {
                                            it.fileType == PlainTextFileType.INSTANCE -> true
                                            it.name.endsWith(".xlsx") -> true
                                            it.name.endsWith(".csv") -> true
                                            else -> false
                                        }
                                    }
                                    FileChooser.chooseFile(fileDescriptor, project, null) {
                                        when {
                                            it.fileType == PlainTextFileType.INSTANCE -> {
                                                input.text = it.inputStream.use { stream ->
                                                    String(
                                                        stream.readAllBytes(),
                                                        StandardCharsets.UTF_8
                                                    )
                                                }
                                            }

                                            it.name.endsWith(".csv") -> {
                                                val confirm = Messages.showOkCancelDialog(
                                                    project,
                                                    "是否跳过第一行",
                                                    "提示",
                                                    "确认",
                                                    "取消",
                                                    AllIcons.Actions.Checked
                                                )
                                                it.inputStream.use { stream ->
                                                    InputStreamReader(stream).use { reader ->
                                                        try {
                                                            val csvData = CsvUtil.getReader().read(reader)
                                                            if(confirm == Messages.OK){
                                                                input.text =
                                                                    csvData
                                                                        .toList()
                                                                        .stream()
                                                                        .skip(1)
                                                                        .toList()
                                                                        .joinToString("\n") { row ->
                                                                            row.rawList.joinToString(" ")
                                                                        }
                                                            }else {
                                                                input.text =
                                                                    csvData
                                                                        .joinToString("\n") { row ->
                                                                            row.rawList.joinToString(" ")
                                                                        }
                                                            }
                                                        } catch (e: Throwable) {
                                                            Helper.getSysLogger(project.locationHash)
                                                                .info(e.message + "\n" + e.stackTrace.joinToString("\n") { err -> err.toString() })
                                                        }
                                                    }
                                                }

                                            }

                                            it.name.endsWith(".xlsx") -> {
                                                it.inputStream.use { stream ->
                                                    try{
                                                        input.text = ExcelUtil.getReader(stream).readAsText(false)
                                                    }catch (e:Throwable){
                                                        Helper.getSysLogger(project.locationHash)
                                                            .info(e.message + "\n" + e.stackTrace.joinToString("\n") { err -> err.toString() })
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }
                            }, BorderLayout.EAST)
                        }, BorderLayout.NORTH)
                        this.add(input, BorderLayout.CENTER)
                    }
                    splitter.secondComponent = JPanel(BorderLayout()).apply {
                        this.add(JPanel().apply {
                            this.layout = BoxLayout(this, BoxLayout.X_AXIS)
                            this.add(JPanel(BorderLayout()).apply {
                                this.add(JLabel("分隔符:"), BorderLayout.WEST)
                                this.add(splitterTextField)
                            })
                            this.add(JPanel(BorderLayout()).apply {
                                this.add(JLabel("转换:"), BorderLayout.WEST)
                                this.add(mapTextField)
                            })
                            this.add(JPanel(BorderLayout()).apply {
                                this.add(JLabel("连接:"), BorderLayout.WEST)
                                this.add(joinTextField)
                            })
//                            this.add(comboBox)
                            this.add(JButton("处理").apply {
                                this.addActionListener {
                                    val text = input.text
                                    if (text.isNotBlank()) {
                                        try {
                                            val splitStr = StringEscapeUtils.unescapeJava(splitterTextField.text)
                                            val outputStr = text.split(splitStr).filter { it.isNotBlank() }
                                                .joinToString(joinTextField.text) {
                                                    mapTextField.text.replace("\${value}", it.trim())
                                                }
                                            output.text = outputStr
                                        } catch (e: Throwable) {
                                            output.text =
                                                e.message + "\n" + e.stackTrace.joinToString("\n") { it.toString() }
                                        }
                                    } else {
                                        Notifications.Bus.notify(
                                            Notification(
                                                "",
                                                "请输入要处理的内容",
                                                NotificationType.WARNING
                                            )
                                        )
                                    }
                                }
                            })
                        }, BorderLayout.NORTH)
                        this.add(output, BorderLayout.CENTER)
                    }
                    splitter.dividerWidth = 1


                    // 创建并配置弹出窗口


                    splitter.addComponentListener(object : ComponentAdapter() {
                        override fun componentResized(e: ComponentEvent?) {
                            splitter.proportion = 0.5f
                        }
                    })
                    splitter
                })
            }
        }
    }

    override fun pluginName(): String = "文本处理"

    override fun pluginDesc(): String = "文本处理"

    override fun pluginVersion(): String = "0.0.2"
}