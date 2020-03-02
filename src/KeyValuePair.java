class KeyValuePair implements Comparable<KeyValuePair>{
    int key, value;

    public KeyValuePair(int key, int value) {
        super();
        this.key = key;
        this.value = value;
    }

    public int compareTo(KeyValuePair o) {
        return value==o.value ? key-o.key:o.value-value;
    }
}
