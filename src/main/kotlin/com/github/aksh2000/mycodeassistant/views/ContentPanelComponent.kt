package com.github.aksh2000.mycodeassistant.views

import com.github.aksh2000.mycodeassistant.control.actions.Action.EXPLAIN_CODE
import com.github.aksh2000.mycodeassistant.control.actions.Action.GENERATE_UNIT_TEST_CASES
import com.github.aksh2000.mycodeassistant.control.actions.ChatBotActionService
import com.intellij.openapi.ui.NullableComponent
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * @author akshithvarma
 */


class ContentPanelComponent(private val chatBotActionService: ChatBotActionService) :
    JBPanel<ContentPanelComponent>(), NullableComponent {

    private var progressBar: JProgressBar
    private val myTitle = JBLabel("Conversation")
    private val myList = JPanel(VerticalLayout(JBUI.scale(10)))
    private val mainPanel = JPanel(BorderLayout(0, JBUI.scale(8)))
    private var lastPrompt = "";
    private val myScrollPane = JBScrollPane(
        myList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    )

    init {
        val splitter = OnePixelSplitter(true, .98f)
        splitter.dividerWidth = 2

        myTitle.foreground = JBColor.namedColor("Label.infoForeground", JBColor(Gray.x80, Gray.x8C))
        myTitle.font = JBFont.label()

        layout = BorderLayout(JBUI.scale(7), 0)
        background = UIUtil.getListBackground()
        mainPanel.isOpaque = false
        add(mainPanel, BorderLayout.CENTER)

        myList.isOpaque = true
        myList.background = UIUtil.getListBackground()
        myScrollPane.border = JBEmptyBorder(10, 15, 10, 15)

        splitter.firstComponent = myScrollPane

        progressBar = JProgressBar()
        splitter.secondComponent = progressBar
        mainPanel.add(splitter)
        myScrollPane.verticalScrollBar.autoscrolls = true
        addQuestionArea()
    }

    fun add(message: String, isMe: Boolean = false, requestPrompt: String?) {
        val messageComponent = MessageComponent(message, isMe)
        val jbScrollPane = JBScrollPane(
            messageComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )

        myList.add(jbScrollPane)
        progressBar.isIndeterminate = true
        if (requestPrompt != null) {
            lastPrompt = requestPrompt
        }
        updateUI()
    }

    fun updateMessage(message: String) {
        myList.remove(myList.componentCount - 1)
        val messageComponent = MessageComponent(message, false)
        myList.add(messageComponent)
        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        updateUI()
    }

    override fun isNull(): Boolean {
        return !isVisible
    }

    fun updateReplaceableContent(content: String, replaceSelectedText: () -> Unit) {
        myList.remove(myList.componentCount - 1)
        val messageComponent = MessageComponent(content, false)

        val jButton = JButton("Replace Selection")
        val listener = ActionListener {
            replaceSelectedText()
            myList.remove(myList.componentCount - 1)
        }
        jButton.addActionListener(listener)
        myList.add(messageComponent)
        myList.add(jButton)
        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        updateUI()
    }

    private fun addQuestionArea() {
        val actionPanel = JPanel(BorderLayout())

        val searchTextArea = JTextField()

        val listener: (ActionEvent) -> Unit = {
            val prompt = searchTextArea.text
            searchTextArea.text = ""
            chatBotActionService.setActionType(EXPLAIN_CODE)
            chatBotActionService.handlePromptAndResponse(this, object : PromptFormatter {
                override fun getUIPrompt() = prompt
                override fun getRequestPrompt() = prompt
            })
        }
        searchTextArea.addActionListener(listener)

        val refreshListener: (ActionEvent) -> Unit = {
            val prompt = lastPrompt
            searchTextArea.text = ""
            chatBotActionService.setActionType(GENERATE_UNIT_TEST_CASES)
            chatBotActionService.handlePromptAndResponse(this, object : PromptFormatter {
                override fun getUIPrompt() = prompt
                override fun getRequestPrompt() = prompt
            })
        }
        searchTextArea.addActionListener(refreshListener)
        actionPanel.add(searchTextArea, BorderLayout.CENTER)

        val actionButtons = JPanel(BorderLayout())
        val clearChat = LinkLabel<String>("Reset", null)
        clearChat.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                myList.removeAll()
                updateUI()
            }
        })
        clearChat.border = JBEmptyBorder(5, 5, 5, 5)

        val button = JButton("Send")
        val refresh = JButton("Retry")
        refresh.addActionListener(refreshListener);
        button.addActionListener(listener)

        actionButtons.add(button, BorderLayout.NORTH)
        actionButtons.add(refresh, BorderLayout.WEST)
        actionButtons.add(clearChat, BorderLayout.SOUTH)
        actionPanel.add(actionButtons, BorderLayout.EAST)

        mainPanel.add(actionPanel, BorderLayout.SOUTH)
    }
}
