package com.lhstack.text

import com.intellij.icons.AllIcons
import com.intellij.json.json5.Json5FileType
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
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
import javax.swing.*


class PluginImpl : IPlugin {

    companion object {
        val CACHE = mutableMapOf<String, JComponent>()
        val DISPOSERS = mutableMapOf<String, Disposable>()
    }

    override fun pluginIcon(): Icon = Helper.findIcon("pluginIcon.svg",PluginImpl::class.java)

    override fun pluginTabIcon(): Icon = Helper.findIcon("pluginTabIcon.svg",PluginImpl::class.java)

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
                    val splitterTextField = JTextField("\\n").apply { this.toolTipText = "默认\\n作为分隔符" }
                    val mapTextField =
                        JTextField("'\${value}'").apply { this.toolTipText = "\${value}作为默认填充变量" }
                    val joinTextField = JTextField(",").apply { this.toolTipText = ",作为默认连接符号" }
                    splitter.firstComponent = input
                    splitter.secondComponent = JPanel(BorderLayout()).apply {
                        this.add(JPanel().apply {
                            val comboBox = JComboBox(
                                arrayOf(
                                    "JSON",
                                    "TEXT",
                                    "SQL",
                                    "JAVA",
                                    "HTML",
                                    "SHELL",
                                    "GROOVY",
                                    "KOTLIN",
                                    "XML"
                                )
                            )
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
                                            val outputStr = text.split(splitStr).joinToString(joinTextField.text) {
                                                mapTextField.text.replace("\${value}", it)
                                            }
                                            output.text = outputStr
                                        } catch (e: Throwable) {
                                           output.text = e.message + "\n" + e.stackTrace.joinToString("\n"){it.toString()}
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
                            splitter.proportion = 0.42f
                        }
                    })
                    splitter
                })
            }
        }
    }

    override fun pluginName(): String = "文本处理"

    override fun pluginDesc(): String = "文本处理"

    override fun pluginVersion(): String = "0.0.1"
}