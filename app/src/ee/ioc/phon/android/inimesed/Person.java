package ee.ioc.phon.android.inimesed;

public class Person {

	private final long mId;
	private final String mKey;
	private final String mDisplayName;

	public Person(long id, String key, String displayName) {
		mId = id;
		mKey = key;
		mDisplayName = displayName;
	}

	public Long getId() {
		return mId;
	}

	public String getKey() {
		return mKey;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public String toString() {
		return getDisplayName();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mId ^ (mId >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Person other = (Person) obj;
		if (mId != other.mId)
			return false;
		return true;
	}
}