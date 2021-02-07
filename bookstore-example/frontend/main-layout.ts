import {css, customElement, html, LitElement, property} from 'lit-element';
import {router} from './index';

// Import global styles of the theme
import '@vaadin/vaadin-lumo-styles/all-imports';

import '@vaadin/vaadin-app-layout/theme/lumo/vaadin-app-layout';
import '@vaadin/vaadin-tabs/theme/lumo/vaadin-tab';
import '@vaadin/vaadin-tabs/theme/lumo/vaadin-tabs';

import * as counterEndpoint from './generated/CounterEndpoint';

interface MenuTab {
  route: string;
  name: string;
}

const menuTabs: MenuTab[] = [
  {route: 'dashboard', name: 'Dashboard'},
  {route: 'Push', name: 'Push'},
];

@customElement('main-layout')
export class MainLayoutElement extends LitElement {
  @property({type: Object}) location = router.location;

  static get styles() {
    return css`
      :host {
        display: block;
        height: 100%;
      }
    `;
  }

  render() {
    return html`
      <vaadin-app-layout id="layout">
      <!--
        <vaadin-tabs slot="navbar" id="tabs" .selected="${this.getIndexOfSelectedTab()}">
          ${menuTabs.map(menuTab => html`
            <vaadin-tab>
              <a href="${menuTab.route}" tabindex="-1">${menuTab.name}</a>
            </vaadin-tab>
          `)}
        </vaadin-tabs>
        -->
        <slot></slot>
      </vaadin-app-layout>
    `;
  }

  private isCurrentLocation(route: string): boolean {
    return router.urlForPath(route) === this.location.getUrl();
  }

  private getIndexOfSelectedTab(): number {
     counterEndpoint.addTen(1)
         .then(result => console.log("addTen should work", result))
         .catch(function (err) {
                 console.log('addTen expected error', err.message); // never called
             });
     counterEndpoint.square(9)
         .then(result => console.log("square should not work", result))
         .catch(function (err) {
                 console.log('square expected error', err.message); // never called
             });
   const index = menuTabs.findIndex(
      menuTab => this.isCurrentLocation(menuTab.route)
    );

    // Select first tab if there is no tab for home in the menu
    if (index === -1 && this.isCurrentLocation('')) {
      return 0;
    }

    return index;
  }
}