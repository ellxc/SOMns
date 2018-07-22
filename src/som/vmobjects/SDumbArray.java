package som.vmobjects;

import com.oracle.truffle.api.profiles.ValueProfile;


public abstract class SDumbArray extends SAbstractObject {
  public static final int FIRST_IDX = 0;
  protected Object[]      storage;
  protected final SClass  clazz;

  public SDumbArray(final long length, final SClass clazz) {
    storage = new Object[(int) length];
    this.clazz = clazz;
  }

  public SDumbArray(final Object[] storage, final SClass clazz) {
    assert storage != null;
    this.storage = storage;
    this.clazz = clazz;
  }

  @Override
  public final SClass getSOMClass() {
    return clazz;
  }

  private final ValueProfile storageType = ValueProfile.createClassProfile();

  public Object[] getStorage(final ValueProfile storageType) {
    return (Object[]) storageType.profile(storage);
  }

  public static class SMutableDumbArray extends SDumbArray {
    public SMutableDumbArray(final long length, final SClass clazz) {
      super(length, clazz);
    }

    public SMutableDumbArray(final Object[] storage, final SClass clazz) {
      super(storage, clazz);
    }

    public SMutableDumbArray shallowCopy() {
      return new SMutableDumbArray(storage.clone(), clazz);
    }

    public void txSet(final SMutableDumbArray a) {
      storage = a.storage;
    }

    @Override
    public boolean isValue() {
      return false;
    }
  }

  public static final class SImmutableDumbArray extends SDumbArray {

    public SImmutableDumbArray(final long length, final SClass clazz) {
      super(length, clazz);
    }

    public SImmutableDumbArray(final Object[] storage, final SClass clazz) {
      super(storage, clazz);
    }

    @Override
    public boolean isValue() {
      return true;
    }
  }

  public static final class STransferDumbArray extends SMutableDumbArray {
    public STransferDumbArray(final long length, final SClass clazz) {
      super(length, clazz);
    }

    public STransferDumbArray(final Object[] storage, final SClass clazz) {
      super(storage, clazz);
    }

    public STransferDumbArray(final STransferDumbArray old, final SClass clazz) {
      super(cloneStorage(old), clazz);
    }

    private static Object[] cloneStorage(final STransferDumbArray old) {
      return old.storage.clone();

    }

    public STransferDumbArray cloneBasics() {
      return new STransferDumbArray(this, clazz);
    }
  }
}
