package it.polito.tdp.seriea.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import it.polito.tdp.seriea.db.SerieADAO;

public class Model {
	
	private Team squadraSelezionata;
	private Map<Season, Integer>punteggi;//punteggi a fine stagione
	
	private List<Team>squadre;
	private Map<String, Team>squadreIdMap;
	private List<Season>stagioni;
	private Map<Integer, Season>stagioniIdMap;
	
	private List<Season>stagioniConsecutive;
	
	private Graph<Season, DefaultWeightedEdge>grafo;
	
	private List<Season>percorsoBest;

	
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
		this.squadraSelezionata=squadra;
		
		SerieADAO dao= new SerieADAO();
		List<Match> partite= dao.listMatchesTeam(squadra, stagioniIdMap, squadreIdMap);//mi restituisce le partite su cui devo calcolare i punteggi per una 
		                                                                               //stagione per una determinata squadra
		
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
	
	
	public Season caclcolaAnnataDOro() {
		//costruisco il grafo
		this.grafo= new SimpleDirectedWeightedGraph<Season, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		
		//vertici-> tutte le stagioni nelle quli ha giocato la quadra selezionata
		Graphs.addAllVertices(this.grafo, punteggi.keySet());
		
		//archi-> per ogni coppia di stagioni orientato e pesato secondo la differenza punti
		for(Season s1: punteggi.keySet()) {
			for(Season s2 : punteggi.keySet()) {
				if(!s1.equals(s2)) { //ricordiamoci che ogni arco in questo caso verrà considerato 
					                 //due volte, quindi per essere un pelo più efficienti dovremmo considerare che 
					                 //s1 sia < di s2, o fare un comparatore apposta oppure comunque fare un controllo sull'esistenza dell'arco
					int punti1= punteggi.get(s1);
					int punti2= punteggi.get(s2);
					
					if(punti1>punti2) {
						Graphs.addEdge(this.grafo, s2, s1, punti1-punti2);
					}else {
						Graphs.addEdge(this.grafo, s1, s2, punti2-punti1);
					}
				}
			}
		}
		
		//trovo l'annata migliore-> deve avere una differenza pesi massima
		Season migliore= null;
		int max=0;
		for(Season s: grafo.vertexSet()) {
			int valore= pesoStagione(s);
			if(valore>max) {
				max= valore;
				migliore=s;
			}
		}
		return migliore;
		
		
	}

	private int pesoStagione(Season s) {
		//somma dei pesi degli archi entranti meno la somma dei pesi degli archi uscenti.
		int somma=0;
		for(DefaultWeightedEdge e: grafo.incomingEdgesOf(s)) {
			somma= somma+(int)grafo.getEdgeWeight(e);
		}
		
		for(DefaultWeightedEdge e: grafo.outgoingEdgesOf(s)) {
			somma= somma-(int)grafo.getEdgeWeight(e);
		}
		
		return somma;
	}
	
	public List<Season> camminoVirtuoso() {
		
		//trova le stagioni consecutive
		this.stagioniConsecutive= new ArrayList<Season>(punteggi.keySet());
		Collections.sort(this.stagioniConsecutive);
		
		//perpara le variabili utili alla ricorsione
		List<Season>parziale= new ArrayList<Season>();
		this.percorsoBest= new ArrayList<Season>();
		
		//itera al livello zero
		for(Season s: grafo.vertexSet()) {
			parziale.add(s);
			cerca(1, parziale);
			parziale.remove(0);
		}
		
		return percorsoBest;
	}
	
	/*
	 * RICORSIONE
	 * 
	 * soluzione parziale: lista di Seaason (lista di vertici)
	 * 
	 * livello ricorsione: lunghezza della lista
	 * 
	 * casi terminali: -> non trovo altri veritici da aggiungere
	 *                 -> verifica se la lunghezza è massima tra quelli visti finora
	 * 
	 * generazione delle soluzioni: vertici connessi all'ultimo vertice del percorso con arco 
	 *                              orientato nel verso giusto, non ancora considerati, 
	 *                              non ancora parte del percorso e relativi a stagioni consecutive.
	 */
	
	public void cerca(int livello, List<Season>parziale) {
		boolean trovato= false;
		
		//il caso temrinale non possiamo scriverlo ora
		//genero nuove soluzioni e poi valuto il caso terminale
		Season ultimo= parziale.get(livello-1);
		
		for(Season prossimo: Graphs.successorListOf(grafo, ultimo)) {
			if(!parziale.contains(prossimo)) {//la lista non lo deve gia contenere
				if(stagioniConsecutive.indexOf(ultimo)+1==stagioniConsecutive.indexOf(prossimo)) {//devono essere consecutivi
					//candidato accettabile-> fai ricorsione
					trovato=true;
					parziale.add(prossimo);
					
					cerca(livello+1, parziale);
					
					parziale.remove(livello);
				}
			}
		}
		
		//valuta caso terminale
		if(!trovato) {
			if(parziale.size()>percorsoBest.size()) {
				//va rimpiazzato con un clone.
				percorsoBest= new ArrayList<Season>(parziale);//clona il best
			}
		}
		
	}
	
}
