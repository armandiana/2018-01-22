package it.polito.tdp.seriea.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.tdp.seriea.db.SerieADAO;

public class Model {
	
	private Team squadraSelezionata;
	private Map<Season, Integer>punteggi;
	
	private List<Team>squadre;
	private Map<String, Team>squadreIdMap;
	private List<Season>stagioni;
	private Map<Integer, Season>stagioniIdMap;

	
	public Model() {
		SerieADAO dao= new SerieADAO();
		
		this.squadre=dao.listTeams();
		this.squadreIdMap= new HashMap<String, Team>();
		for(Team t: squadre) {
			this.squadreIdMap.put(t.getTeam(), t);
		}
		
		
		this.stagioni=dao.listAllSeasons();
		this.stagioniIdMap= new HashMap<Integer, Season>();
		for(Season s: stagioni) {
			this.stagioniIdMap.put(s.getSeason(), s);
		}
	}
	
	public List<Team>getSquadre(){
		return this.squadre;
	}
	
	public Map<Season, Integer> CalcolaPunteggi(Team squadra) {
		this.punteggi= new HashMap<Season, Integer>();
		SerieADAO dao= new SerieADAO();
		List<Match> partite= dao.listMatchesTeam(squadra, stagioniIdMap, squadreIdMap);//mi restituisce le partite su cui devo calcolare i punteggi per una stagione per una determinata squadra
		
		for(Match m: partite) {//per ogni partita giocata devo aggiornare i punti.
			
			Season stagione= m.getSeason();
			
			int punti=0;
			
			//vediamo se è un pareggio-> di sicuro incrementiamo di un punto
			if(m.getFtr().equals("D")) {
				punti=1;
			}else {
				if((m.getHomeTeam().equals(squadra) && m.getFtr().equals("H"))||
						(m.getAwayTeam().equals(squadra) && m.getFtr().equals("A"))) {
					punti=3;
				}
			}
			
			Integer punteggioAttuale= punteggi.get(stagione);
			
			if(punteggioAttuale==null) {
				punteggioAttuale=0;
			}
			punteggi.put(stagione, punteggioAttuale+punti);
			
		}
		
		return punteggi;
	
	}
	
	
	
	
	
	
	
	
	
	

}
