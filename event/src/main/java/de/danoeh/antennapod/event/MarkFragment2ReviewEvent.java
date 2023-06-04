package de.danoeh.antennapod.event;

/**
 * <pre>
 *     author : whatsthat
 *     date   : 21/05/2023
 *     desc   : xxxx 描述
 * </pre>
 */
public class MarkFragment2ReviewEvent {
//    public boolean hasFragments2Review = false;
    public int position = -1;
//    public MarkFragment2ReviewEvent(boolean hasFragments2Review){
//        this.hasFragments2Review = hasFragments2Review;
//    }
    public MarkFragment2ReviewEvent(int position){
        this.position = position;
    }
}
