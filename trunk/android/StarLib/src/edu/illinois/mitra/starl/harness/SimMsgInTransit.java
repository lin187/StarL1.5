package edu.illinois.mitra.starl.harness;

public class SimMsgInTransit implements Comparable<SimMsgInTransit> {

	private String msg = null;
	private String dest = null;
	private long createTime = 0;
	private long deliverTime = 0;
	
	public SimMsgInTransit(String msg, String dest, int delay, long createTime) {
		this.dest = dest;
		this.createTime = createTime;
		deliverTime = createTime + delay;
		this.msg = msg;
	}
	public String getDest() {
		return dest;
	}
	public String getMsg() {
		return msg;
	}
	public long getCreateTime() {
		return createTime;
	}
	public long getDeliverTime() {
		return deliverTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (createTime ^ (createTime >>> 32));
		result = prime * result + (int) (deliverTime ^ (deliverTime >>> 32));
		result = prime * result + ((dest == null) ? 0 : dest.hashCode());
		result = prime * result + ((msg == null) ? 0 : msg.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimMsgInTransit other = (SimMsgInTransit) obj;
		if (createTime != other.createTime)
			return false;
		if (deliverTime != other.deliverTime)
			return false;
		if (dest == null) {
			if (other.dest != null)
				return false;
		} else if (!dest.equals(other.dest))
			return false;
		if (msg == null) {
			if (other.msg != null)
				return false;
		} else if (!msg.equals(other.msg))
			return false;
		return true;
	}

	public int compareTo(SimMsgInTransit another) {
		return (int) (another.getDeliverTime() - deliverTime);
	}	
}
