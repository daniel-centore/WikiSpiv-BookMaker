package com.wikispiv.bookmaker.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;
import com.wikispiv.bookmaker.Utils;
import com.wikispiv.bookmaker.drawables.Drawable;
import com.wikispiv.bookmaker.drawables.PreviewDrawable;
import com.wikispiv.bookmaker.drawables.WSPage;
import com.wikispiv.bookmaker.enums.TransformAction;

public class EditPanel extends JPanel implements MouseMotionListener, MouseListener, KeyListener, ActionListener
{
    private static final long serialVersionUID = 1L;

    private static final int PREVIEW_SEP = 20;
    private static final int REPAINT_MS = 40;

    private Drawable hoverDrawable = null;

    // This contains all drawables from left and right pages, and is updated during
    // a repaint
    private Drawable selectedDrawable = null;
    private WSMouseEvent selectedEvent = null;
    private Point2D selectedInitialPoint = null;
    private TransformAction selectedAction = TransformAction.NOTHING;
    private int selectedIndex = 0; // Index of the item under the cursor
    private WSMouseEvent lastMouseMoveEvent = null;

    private Timer timer = new Timer(REPAINT_MS, this);

    public EditPanel()
    {
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.addKeyListener(this);

        timer.start();
    }

    public void paintComponent(Graphics g)
    {
        this.setBackground(this.getParent().getBackground());
        super.paintComponent(g);

        Main.drawAllPages();

        Graphics2D g2 = (Graphics2D) g;
        RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHints(rh);

        g2.setColor(Color.black);
        g2.drawString("Add a page to begin", 15, 15);

        int panelWidth = this.getWidth();
        int panelHeight = this.getHeight();

        SpivanykPrefs prefs = Main.getPrefs();

        Rectangle pageCoordsL = getPageCoords(false, PREVIEW_SEP, panelWidth, panelHeight);
        Rectangle pageCoordsR = getPageCoords(true, PREVIEW_SEP, panelWidth, panelHeight);

        WSPage leftPage = prefs.getLeftPage();
        WSPage rightPage = prefs.getRightPage();

        // Remove all the previews except for the current two
        for (WSPage page : prefs.getPages()) {
            if (page == leftPage || page == rightPage) {
                continue;
            }
            page.getDrawables().removeIf(d -> d instanceof PreviewDrawable);
        }

        PreviewDrawable previewDrawable = prefs.getPreviewDrawable();
        boolean leftHasPreview = leftPage != null && leftPage.getDrawables().contains(previewDrawable);
        boolean rightHasPreview = rightPage != null && rightPage.getDrawables().contains(previewDrawable);
        if (!leftHasPreview && !rightHasPreview && leftPage != null) {
            leftPage.getDrawables().add(previewDrawable);
            leftHasPreview = true;
        } else if (leftHasPreview && rightHasPreview) {
            rightPage.getDrawables().removeIf(d -> d == previewDrawable);
            rightHasPreview = false;
        }

        if (leftPage != null) {
            if (leftHasPreview) {
                leftPage.getDrawables().removeIf(d -> d == previewDrawable);
                leftPage.getDrawables().add(previewDrawable);
            }
            leftPage.drawMe(g2, pageCoordsL.getX(), pageCoordsL.getY(), pageCoordsL.getWidth(), pageCoordsL.getHeight(),
                    selectedDrawable, hoverDrawable, previewDrawable, getSize(), true, false);
        }
        if (rightPage != null) {
            if (rightHasPreview) {
                rightPage.getDrawables().removeIf(d -> d == previewDrawable);
                rightPage.getDrawables().add(previewDrawable);
            }
            rightPage.drawMe(g2, pageCoordsR.getX(), pageCoordsR.getY(), pageCoordsR.getWidth(),
                    pageCoordsR.getHeight(), selectedDrawable, hoverDrawable, previewDrawable, getSize(), true, false);
        }
    }

    private Rectangle getPageCoords(boolean isSecondPage, int sepSize, int panelWidth, int panelHeight)
    {
        int panelPageWidth = (panelWidth - sepSize) / 2;
        int x = 0;
        int y = 0;

        SpivanykPrefs prefs = Main.getPrefs();

        Dimension pageSize = new Dimension((int) prefs.getPageWidth(), (int) prefs.getPageHeight());
        Dimension panelPageSize = new Dimension(panelPageWidth, panelHeight);
        Dimension scaled = Utils.getScaledDimension(pageSize, panelPageSize);
        int width = (int) scaled.getWidth();
        int height = (int) scaled.getHeight();

        if (isSecondPage) {
            x = width + sepSize;
        }

        return new Rectangle(x, y, width, height);
    }

    private WSMouseEvent getEvent(MouseEvent e)
    {
        return getEvent(e.getX(), e.getY());
    }

    /**
     * Gets a modified version of this event with the coordinates in pt relative to
     * the page, instead of px
     * 
     * @param panelX
     * @param panelY
     * @return
     */
    private WSMouseEvent getEvent(int panelX, int panelY)
    {
        SpivanykPrefs prefs = Main.getPrefs();
        int panelWidth = this.getWidth();
        int panelHeight = this.getHeight();
        Rectangle pageCoordsL = getPageCoords(false, PREVIEW_SEP, panelWidth, panelHeight);
        Rectangle pageCoordsR = getPageCoords(true, PREVIEW_SEP, panelWidth, panelHeight);

        Rectangle curRect = null;
        WSPage curPage = null;
        if (pageCoordsL.contains(new Point(panelX, panelY))) {
            // We are in left page
            curRect = pageCoordsL;
            curPage = prefs.getLeftPage();
        } else if (pageCoordsR.contains(new Point(panelX, panelY))) {
            // We are in right page
            curRect = pageCoordsR;
            curPage = prefs.getRightPage();
        }
        if (curPage == null) {
            // We are not over a page
            return null;
        }

        double pageX = prefs.getPageWidth() / curRect.getWidth() * (panelX - curRect.getX());
        double pageY = prefs.getPageHeight() / curRect.getHeight() * (panelY - curRect.getY());

        return new WSMouseEvent(curPage, pageX, pageY);
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        WSMouseEvent mouseEvent = getEvent(e);
        if (mouseEvent == null || selectedAction == TransformAction.NOTHING) {
            return;
        }
        if (selectedEvent.getPage() != mouseEvent.getPage()) {
            // We have switched pages!
            selectedEvent.getPage().getDrawables().remove(selectedDrawable);
            mouseEvent.getPage().getDrawables().add(selectedDrawable);
            selectedEvent = new WSMouseEvent(mouseEvent.getPage(), selectedEvent.getX(), selectedEvent.getY());
        }
        if (selectedAction == TransformAction.MOVE) {
            selectedDrawable.move(selectedEvent, mouseEvent, selectedInitialPoint);
        } else {
            selectedDrawable.resize(selectedAction, selectedEvent, mouseEvent, selectedInitialPoint);
        }
        Main.somethingChanged(false);
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        this.requestFocusInWindow();
        WSMouseEvent mouseEvent = getEvent(e);
        setupHighlightedAction(mouseEvent);
        lastMouseMoveEvent = mouseEvent;
    }

    private void setupHighlightedAction(WSMouseEvent event)
    {
        int setCursor = Cursor.DEFAULT_CURSOR;
        if (event == null) {
            return;
        }
        HighlightedSitch highlightedSitch = getHighlightedSitch(event);
        TransformAction shouldDoAction = highlightedSitch.transformAction;
        switch (shouldDoAction)
        {
            case MOVE:
                setCursor = Cursor.HAND_CURSOR;
                break;
            case RESIZE_E:
                setCursor = Cursor.E_RESIZE_CURSOR;
                break;
            case RESIZE_N:
                setCursor = Cursor.N_RESIZE_CURSOR;
                break;
            case RESIZE_NE:
                setCursor = Cursor.NE_RESIZE_CURSOR;
                break;
            case RESIZE_NW:
                setCursor = Cursor.NW_RESIZE_CURSOR;
                break;
            case RESIZE_S:
                setCursor = Cursor.S_RESIZE_CURSOR;
                break;
            case RESIZE_SE:
                setCursor = Cursor.SE_RESIZE_CURSOR;
                break;
            case RESIZE_SW:
                setCursor = Cursor.SW_RESIZE_CURSOR;
                break;
            case RESIZE_W:
                setCursor = Cursor.W_RESIZE_CURSOR;
                break;
            case NOTHING:
                break;
            default:
                throw new RuntimeException("Unhandled case: " + shouldDoAction.name());
        }
        this.hoverDrawable = highlightedSitch.drawable;
        this.setCursor(new Cursor(setCursor));
        Main.somethingChanged(false);
    }

    /**
     * Tells us what is currently underneath the cursor
     * 
     * @param event
     * @return
     */
    private HighlightedSitch getHighlightedSitch(WSMouseEvent event)
    {
        List<HighlightedSitch> possibleActions = new ArrayList<>();
        List<Drawable> reverseDrawables = new ArrayList<>(event.getPage().getDrawables());
        Collections.reverse(reverseDrawables);
        for (Drawable d : reverseDrawables) {
            TransformAction shouldDoAction = d.shouldDoAction((int) event.getX(), (int) event.getY());
            if (shouldDoAction != TransformAction.NOTHING) {
                HighlightedSitch highlightedSitch = new HighlightedSitch(shouldDoAction, d);
                possibleActions.add(highlightedSitch);
                // Automatic preference to preview drawable
                if (d instanceof PreviewDrawable) {
                    return highlightedSitch;
                }
            }
        }
        if (selectedIndex >= possibleActions.size()) {
            selectedIndex = 0;
        }
        if (possibleActions.size() == 0) {
            // Nothing under the cursor
            return new HighlightedSitch(TransformAction.NOTHING, null);
        }
        return possibleActions.get(selectedIndex);
    }

    public void resetSelected()
    {
        selectedDrawable = null;
        selectedAction = TransformAction.NOTHING;
        selectedEvent = null;
        selectedInitialPoint = null;
        selectedIndex = 0;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        WSMouseEvent event = getEvent(e);
        if (event == null) {
            return;
        }
        HighlightedSitch highlightedSitch = getHighlightedSitch(event);
        if (highlightedSitch.transformAction == TransformAction.NOTHING) {
            resetSelected();
        } else {
            selectedDrawable = highlightedSitch.drawable;
            selectedAction = highlightedSitch.transformAction;
            selectedEvent = event;
            selectedInitialPoint = highlightedSitch.drawable.getPoint();
        }

        Main.somethingChanged(false);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        int moveAmt = 1;

        switch (e.getKeyCode())
        {
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                if (selectedDrawable instanceof PreviewDrawable) {
                    ((PreviewDrawable) selectedDrawable).setPreview(null);
                } else {
                    selectedEvent.getPage().getDrawables().remove(selectedDrawable);
                }
                resetSelected();
                Main.somethingChanged();
                break;

            case KeyEvent.VK_DOWN:
                if (selectedDrawable != null) {
                    selectedDrawable.setY(selectedDrawable.getY() + moveAmt);
                }
                Main.somethingChanged();
                break;

            case KeyEvent.VK_UP:
                if (selectedDrawable != null) {
                    selectedDrawable.setY(selectedDrawable.getY() - moveAmt);
                }
                Main.somethingChanged();
                break;

            case KeyEvent.VK_LEFT:
                if (selectedDrawable != null) {
                    selectedDrawable.setX(selectedDrawable.getX() - moveAmt);
                }
                Main.somethingChanged();
                break;

            case KeyEvent.VK_RIGHT:
                if (selectedDrawable != null) {
                    selectedDrawable.setX(selectedDrawable.getX() + moveAmt);
                }
                Main.somethingChanged();
                break;

            case KeyEvent.VK_SHIFT:
                selectedIndex++;
                setupHighlightedAction(lastMouseMoveEvent);
                break;

            case KeyEvent.VK_ESCAPE:
                resetSelected();
                Main.somethingChanged(false);
                break;

            case KeyEvent.VK_ENTER:
                Main.getMain().savePreviewPressed();
                break;
                
            case KeyEvent.VK_PAGE_UP:
                Main.getMain().pageSpinnerSet((int) Main.getPrefs().getCurrentLeftPageIndex() - 2);
                break;
                
            case KeyEvent.VK_PAGE_DOWN:
                Main.getMain().pageSpinnerSet((int) Main.getPrefs().getCurrentLeftPageIndex() + 2);
                break;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
    }

    public boolean isSomethingSelected()
    {
        return selectedDrawable != null;
    }

    public Drawable getSelectedDrawable()
    {
        return selectedDrawable;
    }

    public WSMouseEvent getSelectedEvent()
    {
        return selectedEvent;
    }

    private class HighlightedSitch
    {
        TransformAction transformAction;
        Drawable drawable;

        public HighlightedSitch(TransformAction transformAction, Drawable drawable)
        {
            this.transformAction = transformAction;
            this.drawable = drawable;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // if (e.getSource() == timer) {
        // // This kludge constantly runs something changed
        // try {
        // // Main.somethingChanged(false);
        // this.repaint();
        // } catch (Exception e1) {
        // }
        // }
    }

}
