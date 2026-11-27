/**
 * Adresse de messagerie-service uniquement.
 * Le reste de SkillMatch passe par la passerelle : environment.apiUrl (port 8080).
 * La passerelle ne route pas encore /skillmatch/messagerie, donc ce module
 * appelle messagerie-service directement.
 */
export const messagerieConfig = {
  apiUrl: 'http://localhost:8086'
};
