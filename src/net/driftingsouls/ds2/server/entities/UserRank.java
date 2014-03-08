package net.driftingsouls.ds2.server.entities;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.config.Medals;

/**
 * Der Rang eines Spielers bei einem anderen Spieler.
 */
@Entity
@Table(name = "user_rank")
public class UserRank
{
	/**
	 * Die ID eines User-Rangs.
	 * @author Christopher Jung
	 *
	 */
	@Embeddable
	public static class UserRankKey implements Serializable
	{
		private static final long serialVersionUID = -296524516236609133L;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "owner", nullable = false)
		protected User owner;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "rank_giver", nullable = false)
		protected User rankGiver;

		/**
		 * Konstruktor.
		 */
		public UserRankKey()
		{
			// Hibernate
		}

		/**
		 * Konstruktor.
		 *
		 * @param owner
		 *            Der User
		 * @param rankGiver
		 *            Der NPC, der den Rang vergeben hat
		 */
		public UserRankKey(User owner, User rankGiver)
		{
			this.owner = owner;
			this.rankGiver = rankGiver;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((owner == null) ? 0 : owner.hashCode());
			result = prime * result + ((rankGiver == null) ? 0 : rankGiver.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UserRankKey other = (UserRankKey) obj;
			if (owner == null)
			{
				if (other.owner != null)
					return false;
			}
			else if (!owner.equals(other.owner))
				return false;
			if (rankGiver == null)
			{
				if (other.rankGiver != null)
					return false;
			}
			else if (!rankGiver.equals(other.rankGiver))
				return false;
			return true;
		}
	}

	@Id
	private UserRankKey userRankKey;
	private int rank;

	@SuppressWarnings("unused")
	@Version
	private int version;

	/**
	 * Konstruktor.
	 */
	protected UserRank()
	{
		// Hibernate
	}

	/**
	 * Konstruktor.
	 *
	 * @param key
	 *            Der Schluessel (Verleiher des Rangs, Empfaenger)
	 * @param rank
	 *            Die ID des Ranges
	 */
	public UserRank(UserRankKey key, int rank)
	{
		this.userRankKey = key;
		this.rank = rank;
	}

	/**
	 * Gibt den Spieler zurueck, der den Rang verliehen hat.
	 *
	 * @return Der Spieler
	 */
	public User getRankGiver()
	{
		return this.userRankKey.rankGiver;
	}

	/**
	 * Gibt die ID des konkreten Ranges zurueck.
	 *
	 * @return Die ID
	 */
	public int getRank()
	{
		return this.rank;
	}

	/**
	 * Setzt die ID des konkreten Ranges.
	 *
	 * @param rank
	 *            Die ID
	 */
	public void setRank(int rank)
	{
		this.rank = rank;
	}

	/**
	 * Gibt den Anzeigenamen des Rangs zurueck.
	 *
	 * @return Der Anzeigename
	 */
	public String getName()
	{
		String rangName = Medals.get().rang(this.rank).getName();
		if( this.userRankKey.rankGiver.getAlly() != null )
		{
			rangName = this.userRankKey.rankGiver.getAlly().getRangName(rank);
		}
		return rangName;
    }
}
