import { Router } from '@vaadin/router';
import { routes } from './routes';
import './styles/shared-styles';

export const router = new Router(document.querySelector('#outlet'));
router.setRoutes(routes);
