import { Flow } from '@vaadin/flow-frontend';

const { serverSideRoutes } = new Flow({
  imports: () => import('../target/frontend/generated-flow-imports'),
});

export const routes = [
  // for client-side, place routes below (more info https://vaadin.com/docs/v18/flow/typescript/creating-routes.html)
  {
    path: '',
    component: 'main-view',
    action: async () => {
      await import('./views/main/main-view');
    },
    children: [
      {
        path: '',
        component: 'about-view',
        action: async () => {
          await import('./views/about/about-view');
        },
      },
      {
        path: 'about',
        component: 'about-view',
        action: async () => {
          await import('./views/about/about-view');
        },
      },
      {
        path: 'master-detail',
        component: 'master-detail-view',
        action: async () => {
          await import('./views/masterdetail/master-detail-view');
        },
      },
      {
        path: 'person-form',
        component: 'person-form-view',
        action: async () => {
          await import('./views/personform/person-form-view');
        },
      },
      {
        path: 'address-form',
        component: 'address-form-view',
        action: async () => {
          await import('./views/addressform/address-form-view');
        },
      },
      // for server-side, the next magic line sends all unmatched routes:
      ...serverSideRoutes, // IMPORTANT: this must be the last entry in the array
    ],
  },
];
