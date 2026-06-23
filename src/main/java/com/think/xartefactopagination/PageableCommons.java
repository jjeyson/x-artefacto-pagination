package com.think.xartefactopagination;




public class PageableCommons {

    public final int index;
    public final int size;


    public PageableCommons(int size) {
        this(0, size);
    }    

    public PageableCommons(int index, int size) {
        if (index < 0)
            throw new IllegalArgumentException("Page index must be >= 0 : " + index);
        if (size <= 0)
            throw new IllegalArgumentException("Page size must be > 0 : " + size);
        this.index = index;
        this.size = size;
    }    

    public static PageableCommons of(int index, int size) {
        return new PageableCommons(index, size);
    }

    public static PageableCommons ofSize(int size) {
        return new PageableCommons(size);
    }

    public PageableCommons next() {
        return new PageableCommons(index + 1, size);
    }   
    
    public PageableCommons previous() {
        return index > 0 ? new PageableCommons(index - 1, size) : this;
    }   
    
    public PageableCommons first() {
        return index > 0 ? new PageableCommons(0, size) : this;
    }    
    
    public PageableCommons index(int newIndex) {
        return newIndex != index ? new PageableCommons(newIndex, size) : this;
    }

}
